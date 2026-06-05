package com.campus.trade.dto.product;

import java.time.LocalDateTime;

public class SearchHistoryResponse {

    private final String keyword;
    private final LocalDateTime lastSearchedAt;
    private final long searchCount;

    public SearchHistoryResponse(String keyword, LocalDateTime lastSearchedAt, long searchCount) {
        this.keyword = keyword;
        this.lastSearchedAt = lastSearchedAt;
        this.searchCount = searchCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public LocalDateTime getLastSearchedAt() {
        return lastSearchedAt;
    }

    public long getSearchCount() {
        return searchCount;
    }
}
