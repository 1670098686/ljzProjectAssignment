package com.campus.trade.util;

import com.campus.trade.dto.product.ProductResponse;
import com.campus.trade.model.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setTitle(product.getTitle());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCategory(product.getCategory());
        if (product.getCategoryEntity() != null) {
            response.setCategoryId(product.getCategoryEntity().getId());
            response.setCategoryCode(product.getCategoryEntity().getCode());
            response.setCategoryName(product.getCategoryEntity().getName());
        }
        response.setImages(product.getImages());
        response.setTags(product.getTags());
        response.setStatus(product.getStatus());
        response.setAuditStatus(product.getAuditStatus());
        response.setViewCount(product.getViewCount());
        response.setLikeCount(product.getLikeCount());
        response.setContactInfo(product.getContactInfo());
        response.setLocation(product.getLocation());
        response.setRemark(product.getRemark());
        response.setSeller(UserMapper.toMaskedSummary(product.getSeller()));
        response.setCreateTime(product.getCreateTime());
        response.setAverageRating(0D);
        response.setRatingCount(0L);
        response.setFavorited(false);
        return response;
    }
}
