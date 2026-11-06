package com.mall.product.controller;

import com.mall.common.response.Result;
import com.mall.product.entity.Category;
import com.mall.product.entity.Product;
import com.mall.product.entity.StockOperationLog;
import com.mall.product.repository.CategoryRepository;
import com.mall.product.repository.ProductRepository;
import com.mall.product.repository.StockOperationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private static final int CODE_STOCK_LOCK_WAIT_TIMEOUT = 5401;
    private static final int CODE_STOCK_SQL_TIMEOUT = 5402;
    private static final int CODE_STOCK_INSUFFICIENT = 5403;
    private static final int CODE_STOCK_DEDUCT_FAILED = 5404;
    private static final String ACTION_DEDUCT = "DEDUCT";
    private static final String ACTION_ROLLBACK = "ROLLBACK";
    private static final String STATUS_SUCCESS = "SUCCESS";

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockOperationLogRepository stockOperationLogRepository;

    /**
     * 商品列表
     */
    @GetMapping("/list")
    public Result<?> getProductList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "keyword", required = false) String keyword) {

        List<Product> products;
        if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId);
        } else if (keyword != null && !keyword.isEmpty()) {
            products = productRepository.findByKeyword(keyword);
        } else {
            products = productRepository.findAll();
        }

        // 简单的分页处理
        List<Product> pageProducts = new ArrayList<>();
        if (!products.isEmpty()) {
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, products.size());
            pageProducts = products.subList(start, end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", products.size());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("list", pageProducts);

        return Result.success(result);
    }

    /**
     * 商品详情
     */
    @GetMapping("/detail/{productId}")
    public Result<?> getProductDetail(@PathVariable("productId") Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", product.getId());
        result.put("name", product.getName());
        result.put("price", product.getPrice());
        result.put("stock", product.getStock());
        result.put("productImage", product.getProductImage());
        result.put("description", product.getDescription());
        result.put("categoryId", product.getCategoryId());

        if (product.getCategoryId() != null) {
            Category category = categoryRepository.findById(product.getCategoryId()).orElse(null);
            if (category != null) {
                result.put("categoryName", category.getName());
            }
        }

        return Result.success(result);
    }

    /**
     * 商品分类
     */
    @GetMapping("/categories")
    public Result<?> getCategories() {
        List<Category> categories = categoryRepository.findAll();

        Map<String, Object> result = new HashMap<>();
        result.put("categories", categories);

        return Result.success(result);
    }

    /**
     * 商品搜索
     */
    @GetMapping("/search")
    public Result<?> searchProducts(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        List<Product> products = productRepository.findByKeyword(keyword);

        // 简单的分页处理
        List<Product> pageProducts = new ArrayList<>();
        if (!products.isEmpty()) {
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, products.size());
            pageProducts = products.subList(start, end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", products.size());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("list", pageProducts);

        return Result.success(result);
    }

    /**
     * 扣减商品库存（下单时调用）
     */
    @PostMapping("/stock/deduct")
    @Transactional(timeout = 12)
    public Result<?> deductProductStock(@RequestBody Map<String, Object> request) {
        String orderId = String.valueOf(request.getOrDefault("orderId", ""));
        Object itemsObj = request.get("items");
        if (orderId.isBlank()) {
            return Result.error("订单ID不能为空");
        }
        if (!(itemsObj instanceof Map<?, ?> rawItems) || rawItems.isEmpty()) {
            return Result.error("扣减商品不能为空");
        }

        List<Map.Entry<Long, Integer>> itemEntries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : rawItems.entrySet()) {
            Long productId = toLong(entry.getKey());
            Integer quantity = toInteger(entry.getValue());
            if (productId == null || quantity == null || quantity <= 0) {
                return Result.error("库存扣减参数错误");
            }
            itemEntries.add(new AbstractMap.SimpleEntry<>(productId, quantity));
        }
        // 固定加锁顺序，降低多商品并发下的死锁概率。
        itemEntries.sort(Map.Entry.comparingByKey());
        log.info("stock-deduct-start orderId={} itemCount={}", orderId, itemEntries.size());

        for (Map.Entry<Long, Integer> entry : itemEntries) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (isAlreadyProcessed(orderId, productId, quantity, ACTION_DEDUCT)) {
                log.info("stock-deduct-idempotent-hit orderId={} productId={} quantity={} action={}",
                        orderId, productId, quantity, ACTION_DEDUCT);
                continue;
            }

            long sqlStart = System.currentTimeMillis();
            int updated;
            try {
                updated = productRepository.deductStock(productId, quantity);
            } catch (DataAccessException ex) {
                long cost = System.currentTimeMillis() - sqlStart;
                String rootMessage = findRootMessage(ex);
                if (isLockWaitException(ex)) {
                    log.warn("stock-deduct-sql-lock-timeout orderId={} productId={} quantity={} costMs={} root={}",
                            orderId, productId, quantity, cost, rootMessage);
                    throw businessException(CODE_STOCK_LOCK_WAIT_TIMEOUT, "库存锁等待超时，请稍后重试");
                }
                if (isSqlTimeoutException(ex)) {
                    log.warn("stock-deduct-sql-timeout orderId={} productId={} quantity={} costMs={} root={}",
                            orderId, productId, quantity, cost, rootMessage);
                    throw businessException(CODE_STOCK_SQL_TIMEOUT, "库存扣减超时，请稍后重试");
                }
                log.error("stock-deduct-sql-error orderId={} productId={} quantity={} costMs={} root={}",
                        orderId, productId, quantity, cost, rootMessage, ex);
                throw businessException(CODE_STOCK_DEDUCT_FAILED, "库存扣减失败，请重试");
            }
            long sqlCost = System.currentTimeMillis() - sqlStart;
            log.info("stock-deduct-sql-done orderId={} productId={} quantity={} costMs={} updated={}",
                    orderId, productId, quantity, sqlCost, updated);
            if (updated == 0) {
                Product product = productRepository.findById(productId).orElse(null);
                if (product == null) {
                    return Result.error("商品不存在: " + productId);
                }
                int currentStock = product.getStock() == null ? 0 : product.getStock();
                if (currentStock < quantity) {
                    return Result.error(CODE_STOCK_INSUFFICIENT, "库存不足: " + productId);
                }
                return Result.error(CODE_STOCK_DEDUCT_FAILED, "库存扣减失败，请重试: " + productId);
            }

            try {
                saveOperationLog(orderId, productId, quantity, ACTION_DEDUCT);
            } catch (DataIntegrityViolationException ex) {
                log.info("stock-deduct-idempotent-race orderId={} productId={} quantity={} action={}",
                        orderId, productId, quantity, ACTION_DEDUCT);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("deductTime", new Date());
        log.info("stock-deduct-finish orderId={} itemCount={} status=success", orderId, itemEntries.size());
        return Result.success(result);
    }

    /**
     * 回滚商品库存（订单取消或事务补偿时调用）
     */
    @PostMapping("/stock/rollback")
    @Transactional(timeout = 12)
    public Result<?> rollbackProductStock(@RequestBody Map<String, Object> request) {
        String orderId = String.valueOf(request.getOrDefault("orderId", ""));
        Object itemsObj = request.get("items");
        if (orderId.isBlank()) {
            return Result.error("订单ID不能为空");
        }
        if (!(itemsObj instanceof Map<?, ?> rawItems) || rawItems.isEmpty()) {
            return Result.error("回滚商品不能为空");
        }

        List<Map.Entry<Long, Integer>> itemEntries = new ArrayList<>();
        for (Map.Entry<?, ?> entry : rawItems.entrySet()) {
            Long productId = toLong(entry.getKey());
            Integer quantity = toInteger(entry.getValue());
            if (productId == null || quantity == null || quantity <= 0) {
                return Result.error("库存回滚参数错误");
            }
            itemEntries.add(new AbstractMap.SimpleEntry<>(productId, quantity));
        }
        itemEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<Long, Integer> entry : itemEntries) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (isAlreadyProcessed(orderId, productId, quantity, ACTION_ROLLBACK)) {
                log.info("stock-rollback-idempotent-hit orderId={} productId={} quantity={} action={}",
                        orderId, productId, quantity, ACTION_ROLLBACK);
                continue;
            }

            int updated = productRepository.rollbackStock(productId, quantity);
            if (updated == 0) {
                return Result.error("商品不存在: " + productId);
            }

            try {
                saveOperationLog(orderId, productId, quantity, ACTION_ROLLBACK);
            } catch (DataIntegrityViolationException ex) {
                log.info("stock-rollback-idempotent-race orderId={} productId={} quantity={} action={}",
                        orderId, productId, quantity, ACTION_ROLLBACK);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("rollbackTime", new Date());
        return Result.success(result);
    }

    private Long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean isLockWaitException(Throwable ex) {
        return containsMessage(ex, "Lock wait timeout exceeded")
                || containsMessage(ex, "deadlock")
                || containsType(ex, "CannotAcquireLockException")
                || containsType(ex, "PessimisticLockException")
                || containsType(ex, "LockAcquisitionException");
    }

    private boolean isSqlTimeoutException(Throwable ex) {
        return containsType(ex, "QueryTimeoutException")
                || containsType(ex, "SQLTimeoutException")
                || containsMessage(ex, "Query timeout")
                || containsMessage(ex, "statement timeout")
                || containsMessage(ex, "timeout");
    }

    private boolean containsType(Throwable ex, String simpleName) {
        Throwable current = ex;
        while (current != null) {
            if (current.getClass().getSimpleName().equals(simpleName)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean containsMessage(Throwable ex, String keyword) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String findRootMessage(Throwable ex) {
        Throwable current = ex;
        Throwable last = ex;
        while (current != null) {
            last = current;
            current = current.getCause();
        }
        return last == null ? "unknown" : String.valueOf(last.getMessage());
    }

    private boolean isAlreadyProcessed(String orderId, Long productId, Integer quantity, String actionType) {
        return stockOperationLogRepository
                .findByOrderIdAndProductIdAndActionType(orderId, productId, actionType)
                .map(logRecord -> {
                    if (!STATUS_SUCCESS.equals(logRecord.getStatus())) {
                        return false;
                    }
                    Integer loggedQty = logRecord.getQuantity();
                    if (loggedQty != null && !loggedQty.equals(quantity)) {
                        throw new RuntimeException("库存操作幂等冲突: orderId=" + orderId + ", productId=" + productId);
                    }
                    return true;
                })
                .orElse(false);
    }

    private void saveOperationLog(String orderId, Long productId, Integer quantity, String actionType) {
        StockOperationLog logRecord = new StockOperationLog();
        logRecord.setOrderId(orderId);
        logRecord.setProductId(productId);
        logRecord.setQuantity(quantity);
        logRecord.setActionType(actionType);
        logRecord.setStatus(STATUS_SUCCESS);
        stockOperationLogRepository.save(logRecord);
    }

    private RuntimeException businessException(int code, String message) {
        return new RuntimeException(message + "(code=" + code + ")");
    }
}
