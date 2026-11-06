package com.mall.review.controller;

import com.mall.common.response.Result;
import com.mall.review.entity.Review;
import com.mall.review.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 发表评价
     */
    @PostMapping("/add")
    public Result<?> addReview(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = getCurrentUserId(request);
        Long orderId = toLong(data.get("orderId"));
        Long productId = toLong(data.get("productId"));
        if (orderId == null || orderId <= 0 || productId == null || productId <= 0) {
            throw new RuntimeException("orderId 或 productId 参数错误");
        }
        Integer rating = toInteger(data.get("rating"));
        String content = String.valueOf(data.getOrDefault("content", "")).trim();
        List<String> images = (List<String>) data.get("images");

        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1到5之间");
        }
        if (content.isEmpty()) {
            throw new RuntimeException("请输入评价内容");
        }
        if (!isCompletedOrderProduct(userId, orderId, productId)) {
            throw new RuntimeException("订单未完成或不包含该商品，无法评价");
        }
        if (reviewRepository.existsByUserIdAndOrderIdAndProductId(userId, orderId, productId)) {
            throw new RuntimeException("已评价，不可重复提交");
        }

        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(productId);
        review.setOrderId(orderId);
        review.setRating(rating);
        review.setContent(content);
        if (images != null) {
            review.setImages(String.join(",", images));
        }
        review = reviewRepository.save(review);

        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("reviewId", review.getId());
        result.put("userId", review.getUserId());
        result.put("productId", review.getProductId());
        result.put("orderId", review.getOrderId());
        result.put("rating", review.getRating());
        result.put("content", review.getContent());
        result.put("images", images);
        result.put("createTime", review.getCreatedAt());

        return Result.success(result);
    }

    /**
     * 商品评价列表
     */
    @GetMapping("/list")
    public Result<?> getProductReviews(@RequestParam("productId") Long productId,
                                     @RequestParam(value = "page", defaultValue = "1") Integer page,
                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                     @RequestParam(value = "rating", required = false) Integer rating,
                                     @RequestParam(value = "hasImage", required = false) Boolean hasImage) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.max(pageSize == null ? 10 : pageSize, 1);

        List<Review> reviews;
        if (rating != null) {
            reviews = reviewRepository.findByProductIdAndRating(productId, rating);
        } else {
            reviews = reviewRepository.findByProductId(productId);
        }

        if (Boolean.TRUE.equals(hasImage)) {
            List<Review> withImages = new ArrayList<>();
            for (Review review : reviews) {
                if (review.getImages() != null && !review.getImages().isBlank()) {
                    withImages.add(review);
                }
            }
            reviews = withImages;
        }

        int start = (safePage - 1) * safePageSize;
        int end = Math.min(start + safePageSize, reviews.size());
        List<Review> pageReviews = new ArrayList<>();
        if (start < reviews.size()) {
            pageReviews = reviews.subList(start, end);
        }

        Map<String, Object> summary = calculateReviewSummary(reviews);

        List<Map<String, Object>> reviewList = new ArrayList<>();
        for (Review review : pageReviews) {
            Map<String, Object> reviewInfo = new HashMap<>();
            reviewInfo.put("reviewId", review.getId());
            reviewInfo.put("userId", review.getUserId());
            reviewInfo.put("username", getUserName(review.getUserId())); // 从用户服务获取真实用户名
            reviewInfo.put("avatar", "");
            reviewInfo.put("rating", review.getRating());
            reviewInfo.put("content", review.getContent());
            reviewInfo.put("images", review.getImages() != null && !review.getImages().isEmpty() ? review.getImages().split(",") : new String[0]);
            reviewInfo.put("createTime", review.getCreatedAt());
            reviewInfo.put("reply", null);
            reviewList.add(reviewInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", reviews.size());
        result.put("page", safePage);
        result.put("pageSize", safePageSize);
        result.put("summary", summary);
        result.put("list", reviewList);

        return Result.success(result);
    }

    /**
     * 用户评价列表
     */
    @GetMapping("/user/list")
    public Result<?> getUserReviews(HttpServletRequest request,
                                  @RequestParam(value = "page", defaultValue = "1") Integer page,
                                  @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId(request);
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.max(pageSize == null ? 10 : pageSize, 1);

        List<Review> reviews = reviewRepository.findByUserId(userId);

        int start = (safePage - 1) * safePageSize;
        int end = Math.min(start + safePageSize, reviews.size());
        List<Review> pageReviews = new ArrayList<>();
        if (start < reviews.size()) {
            pageReviews = reviews.subList(start, end);
        }

        List<Map<String, Object>> reviewList = new ArrayList<>();
        for (Review review : pageReviews) {
            Map<String, Object> productInfo = getProductInfo(review.getProductId());
            Map<String, Object> reviewInfo = new HashMap<>();
            reviewInfo.put("reviewId", review.getId());
            reviewInfo.put("productId", review.getProductId());
            reviewInfo.put("productName", productInfo.getOrDefault("name", "商品"));
            reviewInfo.put("productImage", productInfo.getOrDefault("productImage", ""));
            reviewInfo.put("orderId", review.getOrderId());
            reviewInfo.put("orderNo", "ORDER" + review.getOrderId());
            reviewInfo.put("rating", review.getRating());
            reviewInfo.put("content", review.getContent());
            reviewInfo.put("images", review.getImages() != null && !review.getImages().isEmpty() ? review.getImages().split(",") : new String[0]);
            reviewInfo.put("createTime", review.getCreatedAt());
            reviewList.add(reviewInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", reviews.size());
        result.put("page", safePage);
        result.put("pageSize", safePageSize);
        result.put("list", reviewList);

        return Result.success(result);
    }

    /**
     * 可评价商品列表
     */
    @GetMapping("/pending-list")
    public Result<?> getPendingReviews(HttpServletRequest request,
                                     @RequestParam(value = "page", defaultValue = "1") Integer page,
                                     @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId(request);
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.max(pageSize == null ? 10 : pageSize, 1);

        List<Map<String, Object>> pendingList = new ArrayList<>();
        List<Map<String, Object>> completedOrders = getCompletedOrders(userId);
        Set<String> reviewedKeys = new HashSet<>();
        for (Review review : reviewRepository.findByUserId(userId)) {
            reviewedKeys.add(review.getOrderId() + "_" + review.getProductId());
        }

        for (Map<String, Object> orderSummary : completedOrders) {
            Long orderId = toLong(orderSummary.get("orderId"));
            if (orderId == null) {
                continue;
            }
            Map<String, Object> detail = getOrderDetail(userId, orderId);
            if (detail.isEmpty()) {
                continue;
            }
            Object itemsObj = detail.get("items");
            if (!(itemsObj instanceof List<?> items)) {
                continue;
            }
            Date receiveTime = toDate(detail.get("receiveTime"));
            String orderNo = String.valueOf(detail.getOrDefault("orderNo", "ORDER" + orderId));

            for (Object obj : items) {
                if (!(obj instanceof Map<?, ?> rawItem)) {
                    continue;
                }
                Map<String, Object> item = toStringKeyMap(rawItem);
                Long productId = toLong(item.get("productId"));
                if (productId == null) {
                    continue;
                }
                String key = orderId + "_" + productId;
                if (reviewedKeys.contains(key)) {
                    continue;
                }
                Map<String, Object> pendingItem = new HashMap<>();
                pendingItem.put("orderId", orderId);
                pendingItem.put("orderNo", orderNo);
                pendingItem.put("productId", productId);
                pendingItem.put("productName", item.getOrDefault("productName", "商品"));
                pendingItem.put("productImage", item.getOrDefault("productImage", ""));
                pendingItem.put("price", toDouble(item.get("price"), 0D));
                pendingItem.put("quantity", toInteger(item.get("quantity"), 0));
                pendingItem.put("receiveTime", receiveTime == null ? new Date() : receiveTime);
                pendingItem.put("reviewed", false);
                pendingList.add(pendingItem);
            }
        }

        int start = (safePage - 1) * safePageSize;
        int end = Math.min(start + safePageSize, pendingList.size());
        List<Map<String, Object>> pageList = new ArrayList<>();
        if (start < pendingList.size()) {
            pageList = pendingList.subList(start, end);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", pendingList.size());
        result.put("page", safePage);
        result.put("pageSize", safePageSize);
        result.put("list", pageList);

        return Result.success(result);
    }

    private boolean isCompletedOrderProduct(Long userId, Long orderId, Long productId) {
        Map<String, Object> detail = getOrderDetail(userId, orderId);
        if (detail.isEmpty()) {
            return false;
        }
        String status = String.valueOf(detail.getOrDefault("status", ""));
        if (!"COMPLETED".equals(status)) {
            return false;
        }
        Object itemsObj = detail.get("items");
        if (!(itemsObj instanceof List<?> items)) {
            return false;
        }
        for (Object obj : items) {
            if (!(obj instanceof Map<?, ?> rawItem)) {
                continue;
            }
            Map<String, Object> item = toStringKeyMap(rawItem);
            Long currentProductId = toLong(item.get("productId"));
            if (productId.equals(currentProductId)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> getCompletedOrders(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "http://mall-order/api/order/list?page=1&pageSize=200&status=COMPLETED",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !isSuccess(body)) {
                return Collections.emptyList();
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map<?, ?> rawData)) {
                return Collections.emptyList();
            }
            Object listObj = rawData.get("list");
            if (!(listObj instanceof List<?> rawList)) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Map<?, ?> rawMap) {
                    result.add(toStringKeyMap(rawMap));
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> getOrderDetail(Long userId, Long orderId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", String.valueOf(userId));

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "http://mall-order/api/order/detail/" + orderId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !isSuccess(body)) {
                return Collections.emptyMap();
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map<?, ?> rawData)) {
                return Collections.emptyMap();
            }
            return toStringKeyMap(rawData);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> getProductInfo(Long productId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "http://mall-product/api/product/detail/" + productId,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();
            if (body == null || !isSuccess(body)) {
                return Collections.emptyMap();
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map<?, ?> rawData)) {
                return Collections.emptyMap();
            }
            return toStringKeyMap(rawData);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private boolean isSuccess(Map<String, Object> body) {
        Object codeObj = body.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
        return code == 200;
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> rawMap) {
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                data.put(key, entry.getValue());
            }
        }
        return data;
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

    private Integer toInteger(Object value, Integer defaultValue) {
        Integer v = toInteger(value);
        return v == null ? defaultValue : v;
    }

    private Double toDouble(Object value, Double defaultValue) {
        if (value instanceof Number num) {
            return num.doubleValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new RuntimeException("缺少用户信息");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("用户信息格式错误");
        }
    }

    private Date toDate(Object value) {
        if (value instanceof Date date) {
            return date;
        }
        return null;
    }

    private String getUserName(Long userId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "http://mall-user/api/user/info/" + userId,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !isSuccess(body)) {
                return "用户" + userId;
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map<?, ?> rawData)) {
                return "用户" + userId;
            }
            Map<String, Object> data = toStringKeyMap(rawData);
            String username = String.valueOf(data.getOrDefault("username", ""));
            return username.isEmpty() ? "用户" + userId : username;
        } catch (Exception e) {
            return "用户" + userId;
        }
    }

    /**
     * 计算评价统计信息
     */
    private Map<String, Object> calculateReviewSummary(List<Review> reviews) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", reviews.size());

        if (reviews.isEmpty()) {
            summary.put("averageRating", 0);
            summary.put("ratingDistribution", new HashMap<String, Integer>());
            summary.put("hasImageCount", 0);
            return summary;
        }

        // 计算平均评分
        double totalRating = 0;
        int hasImageCount = 0;
        Map<String, Integer> ratingDistribution = new HashMap<>();

        for (Review review : reviews) {
            totalRating += review.getRating();
            if (review.getImages() != null && !review.getImages().isEmpty()) {
                hasImageCount++;
            }
            String ratingKey = review.getRating().toString();
            ratingDistribution.put(ratingKey, ratingDistribution.getOrDefault(ratingKey, 0) + 1);
        }

        summary.put("averageRating", totalRating / reviews.size());
        summary.put("ratingDistribution", ratingDistribution);
        summary.put("hasImageCount", hasImageCount);

        return summary;
    }
}
