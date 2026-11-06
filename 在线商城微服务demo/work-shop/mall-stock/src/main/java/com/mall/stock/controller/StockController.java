package com.mall.stock.controller;

import com.mall.common.response.Result;
import com.mall.stock.entity.Stock;
import com.mall.stock.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    @Autowired
    private StockRepository stockRepository;

    @GetMapping("/{productId}")
    public Result<?> getStock(@PathVariable("productId") Long productId) {
        log.info("Query stock for productId: {}", productId);

        if (productId == null) {
            log.warn("ProductId is null");
            return Result.error("商品ID不能为空");
        }

        Stock stock = stockRepository.findByProductId(productId);

        Map<String, Object> stockInfo = new HashMap<>();
        if (stock == null) {
            stockInfo.put("productId", productId);
            stockInfo.put("stock", 0);
            stockInfo.put("availableStock", 0);
            stockInfo.put("lockedStock", 0);
            stockInfo.put("updateTime", new Date());
        } else {
            stockInfo.put("productId", stock.getProductId());
            stockInfo.put("stock", stock.getAvailableQty() + stock.getLockedQty());
            stockInfo.put("availableStock", stock.getAvailableQty());
            stockInfo.put("lockedStock", stock.getLockedQty());
            stockInfo.put("version", stock.getVersion());
            stockInfo.put("updateTime", stock.getUpdatedAt());
        }

        log.info("Stock info for productId {}: {}", productId, stockInfo);
        return Result.success(stockInfo);
    }

    @PostMapping("/deduct")
    @Transactional
    public Result<?> deductStock(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Map<String, Object> items = (Map<String, Object>) request.get("items");

        log.info("Deduct stock for orderId: {}, items: {}", orderId, items);

        if (orderId == null || orderId.isEmpty()) {
            log.warn("OrderId is empty");
            return Result.error("订单ID不能为空");
        }

        if (items == null || items.isEmpty()) {
            log.warn("Items is empty");
            return Result.error("扣减商品不能为空");
        }

        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Long productId;
            Integer quantity;

            try {
                productId = Long.parseLong(entry.getKey());
            } catch (NumberFormatException e) {
                log.warn("Invalid productId format: {}", entry.getKey());
                return Result.error("商品ID格式错误: " + entry.getKey());
            }

            try {
                quantity = (Integer) entry.getValue();
            } catch (ClassCastException e) {
                log.warn("Invalid quantity type for productId {}: {}", entry.getKey(), entry.getValue());
                return Result.error("商品数量类型错误: " + entry.getKey());
            }

            if (quantity == null || quantity <= 0) {
                log.warn("Invalid quantity for productId {}: {}", productId, quantity);
                return Result.error("商品数量必须大于0: " + productId);
            }

            Stock stock = stockRepository.findByProductId(productId);
            if (stock == null) {
                log.warn("Stock not found for productId: {}", productId);
                return Result.error("商品库存不存在: " + productId);
            }

            if (stock.getAvailableQty() < quantity) {
                log.warn("Insufficient stock for productId: {}, available: {}, required: {}",
                        productId, stock.getAvailableQty(), quantity);
                return Result.error("库存不足: " + productId);
            }

            int result = stockRepository.deductStock(productId, quantity, stock.getVersion());
            if (result == 0) {
                log.warn("Concurrent conflict or insufficient stock for productId: {}, version: {}",
                        productId, stock.getVersion());
                return Result.error("库存扣减失败，请重试: " + productId);
            }

            log.info("Successfully deducted stock for productId: {}, quantity: {}", productId, quantity);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("deductTime", new Date());

        return Result.success(result);
    }

    @PostMapping("/rollback")
    @Transactional
    public Result<?> rollbackStock(@RequestBody Map<String, Object> request) {
        String orderId = (String) request.get("orderId");
        Map<String, Object> items = (Map<String, Object>) request.get("items");

        log.info("Rollback stock for orderId: {}, items: {}", orderId, items);

        if (orderId == null || orderId.isEmpty()) {
            log.warn("OrderId is empty");
            return Result.error("订单ID不能为空");
        }

        if (items == null || items.isEmpty()) {
            log.warn("Items is empty");
            return Result.error("回滚商品不能为空");
        }

        for (Map.Entry<String, Object> entry : items.entrySet()) {
            Long productId;
            Integer quantity;

            try {
                productId = Long.parseLong(entry.getKey());
            } catch (NumberFormatException e) {
                log.warn("Invalid productId format: {}", entry.getKey());
                return Result.error("商品ID格式错误: " + entry.getKey());
            }

            try {
                quantity = (Integer) entry.getValue();
            } catch (ClassCastException e) {
                log.warn("Invalid quantity type for productId {}: {}", entry.getKey(), entry.getValue());
                return Result.error("商品数量类型错误: " + entry.getKey());
            }

            if (quantity == null || quantity <= 0) {
                log.warn("Invalid quantity for productId {}: {}", productId, quantity);
                return Result.error("商品数量必须大于0: " + productId);
            }

            Stock stock = stockRepository.findByProductId(productId);
            if (stock == null) {
                log.warn("Stock not found for productId: {}", productId);
                return Result.error("商品库存不存在: " + productId);
            }

            if (stock.getLockedQty() < quantity) {
                log.warn("Insufficient locked stock for productId: {}, locked: {}, required: {}",
                        productId, stock.getLockedQty(), quantity);
                return Result.error("锁定库存不足，无法回滚: " + productId);
            }

            int result = stockRepository.rollbackStock(productId, quantity, stock.getVersion());
            if (result == 0) {
                log.warn("Concurrent conflict during rollback for productId: {}, version: {}",
                        productId, stock.getVersion());
                return Result.error("库存回滚失败，请重试: " + productId);
            }

            log.info("Successfully rolled back stock for productId: {}, quantity: {}", productId, quantity);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("rollbackTime", new Date());

        return Result.success(result);
    }
}