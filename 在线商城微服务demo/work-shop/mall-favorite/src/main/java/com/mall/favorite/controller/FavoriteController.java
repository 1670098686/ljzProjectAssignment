package com.mall.favorite.controller;

import com.mall.common.response.Result;
import com.mall.favorite.entity.Favorite;
import com.mall.favorite.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 收藏商品
     */
    @PostMapping("/add")
    public Result<?> addFavorite(HttpServletRequest request, @RequestBody Map<String, Object> data) {
        Long userId = getCurrentUserId(request);
        Long productId = toLong(data.get("productId"));
        if (productId == null || productId <= 0) {
            throw new RuntimeException("productId 参数错误");
        }

        // 检查是否已收藏
        if (favoriteRepository.findByUserIdAndProductId(userId, productId).isPresent()) {
            throw new RuntimeException("商品已收藏");
        }

        // 创建收藏记录
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite = favoriteRepository.save(favorite);

        return Result.success(toFavoriteView(favorite));
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/{favoriteId}")
    public Result<?> removeFavorite(HttpServletRequest request, @PathVariable("favoriteId") Long favoriteId) {
        Long userId = getCurrentUserId(request);
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new RuntimeException("收藏记录不存在"));
        if (!userId.equals(favorite.getUserId())) {
            throw new RuntimeException("无权操作该收藏记录");
        }
        favoriteRepository.deleteById(favoriteId);
        return Result.success();
    }

    /**
     * 收藏列表
     */
    @GetMapping("/list")
    public Result<?> getFavoriteList(HttpServletRequest request,
                                   @RequestParam(value = "page", defaultValue = "1") Integer page,
                                   @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        Long userId = getCurrentUserId(request);
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.max(pageSize == null ? 10 : pageSize, 1);

        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        List<Favorite> pageFavorites = new ArrayList<>();
        if (!favorites.isEmpty()) {
            int start = (safePage - 1) * safePageSize;
            int end = Math.min(start + safePageSize, favorites.size());
            if (start < favorites.size()) {
                pageFavorites = favorites.subList(start, end);
            }
        }

        List<Map<String, Object>> favoriteList = new ArrayList<>();
        for (Favorite favorite : pageFavorites) {
            favoriteList.add(toFavoriteView(favorite));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", favorites.size());
        result.put("page", safePage);
        result.put("pageSize", safePageSize);
        result.put("list", favoriteList);

        return Result.success(result);
    }

    private Map<String, Object> toFavoriteView(Favorite favorite) {
        Map<String, Object> favoriteInfo = new HashMap<>();
        favoriteInfo.put("favoriteId", favorite.getId());
        favoriteInfo.put("productId", favorite.getProductId());
        favoriteInfo.put("createTime", favorite.getCreatedAt());

        Map<String, Object> productInfo = getProductInfo(favorite.getProductId());
        favoriteInfo.put("productName", String.valueOf(productInfo.getOrDefault("name", "商品")));
        favoriteInfo.put("productImage", String.valueOf(productInfo.getOrDefault("productImage", "")));
        favoriteInfo.put("price", toDouble(productInfo.get("price"), 0D));
        favoriteInfo.put("stock", toInteger(productInfo.get("stock"), 0));
        favoriteInfo.put("status", toInteger(productInfo.get("status"), 1));
        favoriteInfo.put("statusDesc", toInteger(productInfo.get("stock"), 0) > 0 ? "在售" : "缺货");
        return favoriteInfo;
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
            if (body == null) {
                return new HashMap<>();
            }
            Object codeObj = body.get("code");
            int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : Integer.parseInt(String.valueOf(codeObj));
            if (code != 200) {
                return new HashMap<>();
            }
            Object dataObj = body.get("data");
            if (!(dataObj instanceof Map<?, ?> rawMap)) {
                return new HashMap<>();
            }
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    result.put(key, entry.getValue());
                }
            }
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
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

    private Integer toInteger(Object value, Integer defaultValue) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str);
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

    /**
     * 检查收藏状态
     */
    @GetMapping("/check/{productId}")
    public Result<?> checkFavorite(HttpServletRequest request, @PathVariable("productId") Long productId) {
        Long userId = getCurrentUserId(request);

        Favorite favorite = favoriteRepository.findByUserIdAndProductId(userId, productId).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("isFavorite", favorite != null);
        if (favorite != null) {
            result.put("favoriteId", favorite.getId());
        }

        return Result.success(result);
    }
}
