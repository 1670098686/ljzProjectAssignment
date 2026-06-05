package com.campus.trade.service;

import com.campus.trade.common.PaginatedResponse;
import com.campus.trade.dto.favorite.FavoriteListQuery;
import com.campus.trade.dto.favorite.FavoriteStatusResponse;
import com.campus.trade.dto.favorite.FavoriteSummaryResponse;
import com.campus.trade.dto.product.ProductResponse;

import java.util.List;

public interface FavoriteService {

    void addFavorite(String username, Long productId);

    void removeFavorite(String username, Long productId);

    PaginatedResponse<ProductResponse> listMyFavorites(String username, FavoriteListQuery query);

    void attachFavoriteFlags(String username, List<ProductResponse> responses);

    List<FavoriteStatusResponse> getFavoriteStatus(String username, List<Long> productIds);

    FavoriteSummaryResponse getFavoriteSummary(String username, int topN);
}
