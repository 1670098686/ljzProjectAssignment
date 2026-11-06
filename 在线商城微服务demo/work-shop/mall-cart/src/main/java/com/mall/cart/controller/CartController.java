package com.mall.cart.controller;

import com.mall.common.response.Result;
import com.mall.cart.entity.CartItem;
import com.mall.cart.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartItemRepository cartItemRepository;
    
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 添加商品到购物车
     */
    @PostMapping("/add")
    public Result<?> addToCart(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        Long productId = Long.parseLong(data.get("productId").toString());
        Object quantityObj = data.get("quantity");
        Integer quantity = quantityObj == null ? null : ((Number) quantityObj).intValue();
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("商品数量必须大于0");
        }

        // 检查是否已存在
        CartItem existingItem = cartItemRepository.findByUserIdAndProductId(userId, productId).orElse(null);

        if (existingItem != null) {
            // 已存在，更新数量
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setTotalPrice(existingItem.getUnitPrice() * existingItem.getQuantity());
            existingItem.setUpdatedAt(new Date());
            existingItem = cartItemRepository.save(existingItem);
        } else {
            // 不存在，创建新记录
            CartItem cartItem = new CartItem();
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setChecked(true);

            // 仅使用商品服务返回的真实信息，避免写入默认脏数据。
            Map<String, Object> productInfo = getProductInfo(productId);
            String productName = (String) productInfo.get("name");
            if (productName == null || productName.isBlank()) {
                throw new RuntimeException("商品信息不完整: 缺少商品名称");
            }
            Object priceObj = productInfo.get("price");
            if (!(priceObj instanceof Number)) {
                throw new RuntimeException("商品信息不完整: 缺少商品价格");
            }
            String productImage = (String) productInfo.get("productImage");
            if (productImage == null) {
                productImage = (String) productInfo.get("coverImage");
            }

            cartItem.setProductName(productName);
            cartItem.setProductImage(productImage == null ? "" : productImage);
            cartItem.setUnitPrice(((Number) priceObj).doubleValue());
            
            cartItem.setTotalPrice(cartItem.getUnitPrice() * quantity);
            cartItem.setCreatedAt(new Date());
            cartItem.setUpdatedAt(new Date());
            existingItem = cartItemRepository.save(cartItem);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", existingItem.getId());
        result.put("userId", existingItem.getUserId());
        result.put("productId", existingItem.getProductId());
        result.put("productName", existingItem.getProductName());
        result.put("productImage", existingItem.getProductImage());
        result.put("price", existingItem.getUnitPrice());
        result.put("quantity", existingItem.getQuantity());
        result.put("checked", existingItem.getChecked());
        result.put("createTime", existingItem.getCreatedAt());

        return Result.success(result);
    }

    /**
     * 修改购物车商品数量
     */
    @PutMapping("/update")
    public Result<?> updateCartItem(@RequestBody Map<String, Object> data) {
        Long cartId = Long.parseLong(data.get("cartId").toString());
        Integer quantity = (Integer) data.get("quantity");

        CartItem cartItem = cartItemRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("购物车商品不存在"));

        cartItem.setQuantity(quantity);
        cartItem.setTotalPrice(cartItem.getUnitPrice() * quantity);
        cartItem.setUpdatedAt(new Date());
        cartItem = cartItemRepository.save(cartItem);

        Map<String, Object> result = new HashMap<>();
        result.put("cartId", cartItem.getId());
        result.put("productId", cartItem.getProductId());
        result.put("quantity", cartItem.getQuantity());
        result.put("totalPrice", cartItem.getTotalPrice());

        return Result.success(result);
    }

    /**
     * 删除购物车商品
     */
    @DeleteMapping("/{cartId}")
    public Result<?> deleteCartItem(HttpServletRequest request, @PathVariable("cartId") Long cartId) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));
        
        CartItem cartItem = cartItemRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("购物车商品不存在"));
        
        if (!cartItem.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该商品");
        }
        
        cartItemRepository.deleteById(cartId);
        return Result.success();
    }

    /**
     * 购物车列表
     */
    @GetMapping("/list")
    public Result<?> getCartList(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);

        // 计算购物车汇总信息
        int totalCount = 0;
        int checkedCount = 0;
        double totalPrice = 0;

        for (CartItem item : cartItems) {
            totalCount += item.getQuantity();
            if (item.getChecked()) {
                checkedCount += item.getQuantity();
                totalPrice += item.getTotalPrice();
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("checkedCount", checkedCount);
        summary.put("totalPrice", totalPrice);
        summary.put("totalDiscount", 0);
        summary.put("finalPrice", totalPrice);

        // 转换购物车商品列表，确保返回正确的字段名
        List<Map<String, Object>> cartItemMaps = new ArrayList<>();
        for (CartItem item : cartItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("cartId", item.getId());
            itemMap.put("productId", item.getProductId());
            itemMap.put("productName", item.getProductName());
            itemMap.put("productImage", item.getProductImage());
            itemMap.put("price", item.getUnitPrice());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("checked", item.getChecked());
            itemMap.put("totalPrice", item.getTotalPrice());
            cartItemMaps.add(itemMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("cartItems", cartItemMaps);
        result.put("summary", summary);

        return Result.success(result);
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/clear")
    public Result<?> clearCart(HttpServletRequest request) {
        Long userId = Long.parseLong(request.getHeader("X-User-Id"));

        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        cartItemRepository.deleteAll(cartItems);

        return Result.success();
    }
    
    /**
     * 获取商品信息
     */
    private Map<String, Object> getProductInfo(Long productId) {
        // 调用商品服务获取商品详情
        String url = "http://mall-product/api/product/detail/" + productId;
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null) {
            throw new RuntimeException("调用商品服务失败");
        }
        Object codeObj = response.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
        if (code != 200) {
            throw new RuntimeException("获取商品信息失败: " + response.get("message"));
        }
        Object dataObj = response.get("data");
        if (!(dataObj instanceof Map)) {
            throw new RuntimeException("商品服务返回数据格式错误");
        }
        return (Map<String, Object>) dataObj;
    }
}
