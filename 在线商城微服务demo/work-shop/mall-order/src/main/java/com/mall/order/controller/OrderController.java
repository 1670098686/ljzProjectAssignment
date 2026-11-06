package com.mall.order.controller;

import com.mall.common.response.Result;
import com.mall.order.entity.Order;
import com.mall.order.entity.OrderItem;
import com.mall.order.repository.OrderItemRepository;
import com.mall.order.repository.OrderRepository;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final int STOCK_API_MAX_ATTEMPTS = 3;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("directRestTemplate")
    private RestTemplate directRestTemplate;

    @Value("${mall.product.direct-url:}")
    private String mallProductDirectUrl;

    /**
     * 提交订单
     */
    @PostMapping("/submit")
    @GlobalTransactional(name = "order-submit-tx", rollbackFor = Exception.class)
    public Result<?> submitOrder(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        long submitStart = System.currentTimeMillis();
        Long userId = getCurrentUserId(request);
        Long addressId = Long.parseLong(data.get("addressId").toString());
        List<Long> cartIds = parseCartIds(data.get("cartIds"));
        log.info("order-submit-start userId={} addressId={} cartCount={}", userId, addressId, cartIds.size());
        String remark = (String) data.get("remark");
        String paymentType = (String) data.get("paymentType");
        if (paymentType == null || paymentType.isBlank()) {
            paymentType = "online";
        }
        if (cartIds.isEmpty()) {
            return Result.error(400, "cartIds 不能为空");
        }
    long cartFetchStart = System.currentTimeMillis();
        List<Map<String, Object>> cartItems = new ArrayList<>();
        try {
            cartItems = getCheckedCartItems(userId, cartIds);
        } catch (RuntimeException ex) {
            if (ex.getMessage().contains("购物车商品不存在")) {
                log.warn("所有购物车商品不存在，无法提交订单: {}", ex.getMessage());
                return Result.error(400, "购物车商品不存在，无法提交订单");
            }
            throw ex;
        }
    log.info("order-submit-stage stage=cart-fetch userId={} costMs={} itemCount={}",
        userId, System.currentTimeMillis() - cartFetchStart, cartItems.size());

        String orderNo = generateOrderNo();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(0.0);
        order.setFreight(0.0);
        order.setFinalAmount(0.0);
        order.setStatus("PENDING_PAYMENT");
        order.setPaymentType(paymentType);
        order.setAddressId(addressId);
        order.setRemark(remark);
        order = orderRepository.save(order);
        log.info("order-submit-stage stage=order-created orderId={} userId={}", order.getId(), userId);

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0;

        for (Map<String, Object> cartItem : cartItems) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());

            Long productId = toLong(cartItem.get("productId"));
            Integer quantity = toInteger(cartItem.get("quantity"));
            Double price = toDouble(cartItem.get("price"));
            String productName = String.valueOf(cartItem.getOrDefault("productName", "商品"));
            String productImage = String.valueOf(cartItem.getOrDefault("productImage", ""));

            if (productId == null || quantity == null || quantity <= 0 || price == null) {
                throw new RuntimeException("购物车商品数据不完整，无法创建订单");
            }

            item.setProductId(productId);
            item.setPrice(price);
            item.setQuantity(quantity);
            item.setAmount(price * quantity);
            item.setProductName(productName);
            item.setProductImage(productImage);
            orderItems.add(item);
            totalAmount += item.getAmount();
        }

        orderItemRepository.saveAll(orderItems);
        log.info("order-submit-stage stage=order-items-saved orderId={} itemCount={}", order.getId(), orderItems.size());

        // 更新订单金额
        order.setTotalAmount(totalAmount);
        order.setFinalAmount(totalAmount);
        orderRepository.save(order);
        log.info("order-submit-stage stage=order-amount-saved orderId={} totalAmount={}", order.getId(), totalAmount);

        // 提交时先扣库存，再清理下单购物车，Seata 会自动处理事务回滚。
        long deductStart = System.currentTimeMillis();
        deductProductStock(order.getId());
        log.info("order-submit-stage stage=stock-deduct orderId={} costMs={} status=success",
                order.getId(), System.currentTimeMillis() - deductStart);
        long clearCartStart = System.currentTimeMillis();
        clearSelectedCartItems(userId, cartIds);
        log.info("order-submit-stage stage=cart-clear orderId={} costMs={} cartCount={}",
                order.getId(), System.currentTimeMillis() - clearCartStart, cartIds.size());

        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("id", order.getId());
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("userId", order.getUserId());
        result.put("items", orderItems);
        result.put("totalAmount", order.getTotalAmount());
        result.put("freight", order.getFreight());
        result.put("finalAmount", order.getFinalAmount());
        result.put("status", order.getStatus());
        result.put("statusDesc", "待支付");

        Map<String, Object> address = getAddressInfo(userId, addressId);
        result.put("address", address);

        result.put("createTime", order.getCreatedAt());
        result.put("paymentExpireTime", new Date(System.currentTimeMillis() + 30 * 60 * 1000)); // 30分钟过期

        log.info("order-submit-finish orderId={} userId={} totalCostMs={} status=success",
            order.getId(), userId, System.currentTimeMillis() - submitStart);

        return Result.success(result);
    }

    /**
     * 订单列表
     */
    @GetMapping("/list")
    public Result<?> getOrderList(HttpServletRequest request,
                                 @RequestParam(value = "page", defaultValue = "1") Integer page,
                                 @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                 @RequestParam(value = "status", required = false) String status) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("total", 0);
            result.put("page", page);
            result.put("pageSize", pageSize);
            result.put("list", new ArrayList<>());
            return Result.success(result);
        }
        Long userId = Long.parseLong(userIdHeader);

        List<Order> orders;
        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByUserIdAndStatusOrderByIdDesc(userId, status);
        } else {
            orders = orderRepository.findByUserIdOrderByIdDesc(userId);
        }

        // 简单的分页处理
        List<Order> pageOrders = new ArrayList<>();
        if (!orders.isEmpty()) {
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, orders.size());
            if (start < orders.size()) {
                pageOrders = orders.subList(start, end);
            }
        }

        // 构建响应数据
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Order order : pageOrders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<Map<String, Object>> itemList = new ArrayList<>();
            for (OrderItem item : items) {
                itemList.add(toOrderItemView(item));
            }

            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("orderId", order.getId());
            orderInfo.put("orderNo", order.getOrderNo());
            orderInfo.put("totalAmount", order.getTotalAmount());
            orderInfo.put("status", order.getStatus());
            orderInfo.put("statusDesc", getStatusDesc(order.getStatus()));
            orderInfo.put("itemCount", items.size());
            orderInfo.put("items", itemList);
            orderInfo.put("createTime", order.getCreatedAt());
            orderList.add(orderInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", orders.size());
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("list", orderList);

        return Result.success(result);
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{orderId}")
    public Result<?> getOrderDetail(HttpServletRequest request, @PathVariable("orderId") Long orderId) {
        Long userId = getCurrentUserId(request);
        Order order = getOwnedOrder(orderId, userId);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        List<Map<String, Object>> orderItemViews = new ArrayList<>();
        for (OrderItem item : orderItems) {
            orderItemViews.add(toOrderItemView(item));
        }

        Map<String, Object> address = getAddressInfo(order.getUserId(), order.getAddressId());

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("userId", order.getUserId());
        result.put("items", orderItemViews);
        result.put("totalAmount", order.getTotalAmount());
        result.put("freight", order.getFreight());
        result.put("finalAmount", order.getFinalAmount());
        result.put("status", order.getStatus());
        result.put("statusDesc", getStatusDesc(order.getStatus()));
        result.put("address", address);
        result.put("paymentType", order.getPaymentType());
        result.put("paymentTime", order.getPaymentTime());
        result.put("deliveryTime", order.getDeliveryTime());
        result.put("receiveTime", order.getReceiveTime());
        result.put("createTime", order.getCreatedAt());
        result.put("cancelTime", order.getCancelTime());

        return Result.success(result);
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay")
    public Result<?> payOrder(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = getCurrentUserId(request);
        Long orderId = Long.parseLong(data.get("orderId").toString());
        String paymentType = (String) data.get("paymentType");

        Order order = getOwnedOrder(orderId, userId);

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setStatus("PAID");
        order.setPaymentTime(new Date());
        order.setPaymentType(paymentType);
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("paymentNo", generatePaymentNo());
        result.put("amount", order.getFinalAmount());
        result.put("paymentTime", order.getPaymentTime());
        result.put("status", order.getStatus());

        return Result.success(result);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> cancelOrder(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = getCurrentUserId(request);
        Long orderId = Long.parseLong(data.get("orderId").toString());

        Order order = getOwnedOrder(orderId, userId);

        if (!"PENDING_PAYMENT".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        rollbackProductStock(orderId);

        order.setStatus("CANCELLED");
        order.setCancelTime(new Date());
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("cancelTime", order.getCancelTime());
        result.put("status", order.getStatus());

        return Result.success(result);
    }

    /**
     * 商家发货
     */
    @PostMapping("/ship")
    public Result<?> shipOrder(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = getCurrentUserId(request);
        Long orderId = Long.parseLong(data.get("orderId").toString());

        Order order = getOwnedOrder(orderId, userId);
        if (!"PAID".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setStatus("SHIPPED");
        order.setDeliveryTime(new Date());
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("deliveryTime", order.getDeliveryTime());
        result.put("status", order.getStatus());
        return Result.success(result);
    }

    /**
     * 确认收货
     */
    @PostMapping("/confirm")
    public Result<?> confirmReceipt(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = getCurrentUserId(request);
        Long orderId = Long.parseLong(data.get("orderId").toString());

        Order order = getOwnedOrder(orderId, userId);

        if (!"SHIPPED".equals(order.getStatus()) && !"PAID".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不正确");
        }

        order.setStatus("COMPLETED");
        order.setReceiveTime(new Date());
        orderRepository.save(order);

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("receiveTime", order.getReceiveTime());
        result.put("status", order.getStatus());

        return Result.success(result);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORDER" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /**
     * 生成支付单号
     */
    private String generatePaymentNo() {
        return "PAY" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /**
     * 解析购物车ID列表，兼容前端传入的 Integer/Long/String。
     */
    private List<Long> parseCartIds(Object cartIdsObj) {
        if (!(cartIdsObj instanceof List<?> rawList)) {
            throw new RuntimeException("cartIds 参数格式错误");
        }
        List<Long> cartIds = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Number num) {
                cartIds.add(num.longValue());
                continue;
            }
            if (item instanceof String str && !str.isBlank()) {
                cartIds.add(Long.parseLong(str));
                continue;
            }
            throw new RuntimeException("cartIds 参数格式错误");
        }
        return cartIds;
    }

    /**
     * 从购物车服务读取已勾选商品，并按 cartIds 过滤。
     */
    private List<Map<String, Object>> getCheckedCartItems(Long userId, List<Long> cartIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "http://mall-cart/api/cart/list",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("调用购物车服务失败");
        }

        Object codeObj = body.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
        if (code != 200) {
            throw new RuntimeException("获取购物车信息失败: " + body.get("message"));
        }

        Object dataObj = body.get("data");
        if (!(dataObj instanceof Map<?, ?> dataMap)) {
            throw new RuntimeException("购物车返回数据格式错误");
        }

        Object cartItemsObj = dataMap.get("cartItems");
        if (!(cartItemsObj instanceof List<?> rawCartItems)) {
            throw new RuntimeException("购物车商品数据格式错误");
        }

        Set<Long> cartIdSet = new HashSet<>(cartIds);
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Object itemObj : rawCartItems) {
            if (!(itemObj instanceof Map<?, ?> rawItem)) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawItem.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    item.put(key, entry.getValue());
                }
            }

            Long cartId = toLong(item.get("cartId"));
            Boolean checked = toBoolean(item.get("checked"));
            if (cartId != null && cartIdSet.contains(cartId) && Boolean.TRUE.equals(checked)) {
                selected.add(item);
            }
        }

        if (selected.isEmpty()) {
            throw new RuntimeException("购物车商品不存在或未勾选，无法提交订单");
        }
        if (selected.size() != cartIds.size()) {
            log.warn("部分购物车商品不存在或未勾选，只处理存在的商品: 请求{}个，实际{}个", cartIds.size(), selected.size());
        }
        return selected;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new RuntimeException("未登录或缺少用户信息");
        }
        return Long.parseLong(userIdHeader);
    }

    private Order getOwnedOrder(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("订单不存在或无权限访问"));
    }

    private void clearSelectedCartItems(Long userId, List<Long> cartIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", String.valueOf(userId));

        for (Long cartId : cartIds) {
            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        "http://mall-cart/api/cart/" + cartId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<Map<String, Object>>() {}
                );
                Map<String, Object> body = response.getBody();
                if (body == null) {
                    log.warn("清理购物车失败: cartId={}，响应为空", cartId);
                    continue; // 跳过空响应，继续清理其他购物车商品
                }
                Object codeObj = body.get("code");
                int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
                if (code != 200) {
                    String message = String.valueOf(body.get("message"));
                    if (message.contains("购物车商品不存在")) {
                        log.info("购物车商品已不存在: cartId={}", cartId);
                        continue; // 购物车商品已不存在，跳过
                    }
                    throw new RuntimeException("清理购物车失败: " + message);
                }
            } catch (RuntimeException ex) {
                log.warn("清理购物车失败: cartId={}，原因={}", cartId, ex.getMessage());
                // 继续清理其他购物车商品，不中断整个流程
            }
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number num) {
            return num.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            return Long.parseLong(str);
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            return Integer.parseInt(str);
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            return Double.parseDouble(str);
        }
        return null;
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String str) {
            return Boolean.parseBoolean(str);
        }
        return null;
    }

    /**
     * 订单明细展示兜底：历史数据中若名称/图片是占位值，按 productId 回查商品服务补齐。
     */
    private Map<String, Object> toOrderItemView(OrderItem item) {
        Map<String, Object> itemInfo = new HashMap<>();
        itemInfo.put("id", item.getId());
        itemInfo.put("orderId", item.getOrderId());
        itemInfo.put("productId", item.getProductId());
        itemInfo.put("price", item.getPrice());
        itemInfo.put("quantity", item.getQuantity());
        itemInfo.put("amount", item.getAmount());

        String productName = item.getProductName();
        String productImage = item.getProductImage();
        boolean namePlaceholder = productName == null || productName.isBlank() || "商品名称".equals(productName);
        boolean imageMissing = productImage == null || productImage.isBlank();

        if (namePlaceholder || imageMissing) {
            Map<String, Object> productInfo = getProductInfo(item.getProductId());
            if (namePlaceholder) {
                Object nameObj = productInfo.get("name");
                if (nameObj instanceof String name && !name.isBlank()) {
                    productName = name;
                }
            }
            if (imageMissing) {
                Object imageObj = productInfo.get("productImage");
                if (imageObj instanceof String image && !image.isBlank()) {
                    productImage = image;
                }
            }
        }

        itemInfo.put("productName", productName == null ? "商品" : productName);
        itemInfo.put("productImage", productImage == null ? "" : productImage);
        return itemInfo;
    }

    private Map<String, Object> getProductInfo(Long productId) {
        if (productId == null) {
            return Collections.emptyMap();
        }
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "http://mall-product/api/product/detail/" + productId,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body == null) {
                return Collections.emptyMap();
            }
            Object codeObj = body.get("code");
            int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
            if (code != 200) {
                return Collections.emptyMap();
            }
            Object dataObj = body.get("data");
            if (dataObj instanceof Map<?, ?> rawData) {
                Map<String, Object> data = new HashMap<>();
                for (Map.Entry<?, ?> entry : rawData.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        data.put(key, entry.getValue());
                    }
                }
                return data;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 按 addressId 获取用户真实收货地址。
     */
    private Map<String, Object> getAddressInfo(Long userId, Long addressId) {
        if (userId == null || addressId == null) {
            return Collections.emptyMap();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "http://mall-user/api/user/addresses",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return Collections.emptyMap();
            }
            Object codeObj = body.get("code");
            int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
            if (code != 200) {
                return Collections.emptyMap();
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof List<?> rawList)) {
                return Collections.emptyMap();
            }

            for (Object obj : rawList) {
                if (!(obj instanceof Map<?, ?> rawMap)) {
                    continue;
                }
                Map<String, Object> address = new HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    if (entry.getKey() instanceof String key) {
                        address.put(key, entry.getValue());
                    }
                }
                Long id = toLong(address.get("id"));
                if (addressId.equals(id)) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("receiver", address.getOrDefault("name", ""));
                    result.put("phone", address.getOrDefault("phone", ""));
                    result.put("province", address.getOrDefault("province", ""));
                    result.put("city", address.getOrDefault("city", ""));
                    result.put("district", address.getOrDefault("district", ""));
                    result.put("detailAddress", address.getOrDefault("detail", ""));
                    return result;
                }
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * 支付时扣减商品服务库存，失败则终止支付流程。
     */
    private void deductProductStock(Long orderId) {
        Map<String, Object> items = buildProductStockItems(orderId);
        if (items.isEmpty()) {
            throw new RuntimeException("订单商品不存在，无法扣减库存");
        }

        Map<String, Object> request = new HashMap<>();
        request.put("orderId", String.valueOf(orderId));
        request.put("items", items);

        log.info("order-stock-deduct-call orderId={} productIds={} itemCount={}",
            orderId, items.keySet(), items.size());

        long callStart = System.currentTimeMillis();

        ResponseEntity<Map<String, Object>> response = callProductStockApiWithRetry(
            "http://mall-product/api/product/stock/deduct",
            request,
            "扣减库存"
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("扣减库存失败: 商品服务无响应");
        }
        Object codeObj = body.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
        String message = String.valueOf(body.getOrDefault("message", "未知错误"));
        log.info("order-stock-deduct-response orderId={} costMs={} code={} message={}",
                orderId, System.currentTimeMillis() - callStart, code, message);
        if (code != 200) {
            throw new RuntimeException("扣减库存失败(code=" + code + "): " + message);
        }
    }

    private void rollbackProductStock(Long orderId) {
        Map<String, Object> items = buildProductStockItems(orderId);
        if (items.isEmpty()) {
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("orderId", String.valueOf(orderId));
        request.put("items", items);

        ResponseEntity<Map<String, Object>> response = callProductStockApiWithRetry(
            "http://mall-product/api/product/stock/rollback",
            request,
            "回滚库存"
        );

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("回滚库存失败: 商品服务无响应");
        }
        Object codeObj = body.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
        if (code != 200) {
            throw new RuntimeException("回滚库存失败: " + body.get("message"));
        }
    }

    private Map<String, Object> buildProductStockItems(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        Map<String, Object> items = new HashMap<>();
        for (OrderItem item : orderItems) {
            String key = String.valueOf(item.getProductId());
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            int mergedQty = qty;
            Object exists = items.get(key);
            if (exists instanceof Number num) {
                mergedQty += num.intValue();
            }
            items.put(key, mergedQty);
        }
        return items;
    }

    private ResponseEntity<Map<String, Object>> callProductStockApiWithRetry(
            String url,
            Map<String, Object> request,
            String action
    ) {
        List<String> candidateUrls = buildProductApiCandidateUrls(url);
        Exception lastException = null;
        for (String candidateUrl : candidateUrls) {
            for (int attempt = 1; attempt <= STOCK_API_MAX_ATTEMPTS; attempt++) {
                long attemptStart = System.currentTimeMillis();
                try {
                    RestTemplate stockApiRestTemplate = selectRestTemplate(candidateUrl);
                    ResponseEntity<Map<String, Object>> response = stockApiRestTemplate.exchange(
                            candidateUrl,
                            HttpMethod.POST,
                            new HttpEntity<>(request),
                            new ParameterizedTypeReference<Map<String, Object>>() {}
                    );
                    log.info("order-stock-api-attempt action={} url={} attempt={} costMs={} status={}",
                            action, candidateUrl, attempt, System.currentTimeMillis() - attemptStart, response.getStatusCode().value());
                    return response;
                } catch (ResourceAccessException ex) {
                    log.warn("order-stock-api-attempt action={} url={} attempt={} costMs={} status=timeout reason={}",
                            action, candidateUrl, attempt, System.currentTimeMillis() - attemptStart, ex.getMessage());
                    lastException = ex;
                } catch (Exception ex) {
                    log.warn("order-stock-api-attempt action={} url={} attempt={} costMs={} status=error reason={}",
                            action, candidateUrl, attempt, System.currentTimeMillis() - attemptStart, ex.getMessage());
                    lastException = ex;
                }
            }
        }
        // 返回一个表示失败的响应，而不是抛出异常
        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("code", 5402);
        errorBody.put("message", action + "失败(code=5402): 商品服务连接超时");
        errorBody.put("data", null);
        return new ResponseEntity<>(errorBody, HttpStatus.OK);
    }

    private RestTemplate selectRestTemplate(String url) {
        if (url.startsWith("http://mall-product") || url.startsWith("https://mall-product")) {
            return restTemplate;
        }
        return directRestTemplate;
    }

    private List<String> buildProductApiCandidateUrls(String originalUrl) {
        List<String> urls = new ArrayList<>();
        // 首先尝试直接IP地址
        String direct = mallProductDirectUrl == null ? "" : mallProductDirectUrl.trim();
        if (!direct.isEmpty()) {
            if (direct.endsWith("/")) {
                direct = direct.substring(0, direct.length() - 1);
            }
            String directUrl = originalUrl.replaceFirst("^http://mall-product", direct);
            if (!directUrl.equals(originalUrl)) {
                urls.add(directUrl);
            }
        }
        // 然后尝试服务名
        urls.add(originalUrl);
        return urls;
    }

    /**
     * 获取订单状态描述
     */
    private String getStatusDesc(String status) {
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("PENDING_PAYMENT", "待支付");
        statusMap.put("PAID", "待发货");
        statusMap.put("SHIPPED", "待收货");
        statusMap.put("COMPLETED", "已完成");
        statusMap.put("CANCELLED", "已取消");
        return statusMap.getOrDefault(status, "未知状态");
    }
}
