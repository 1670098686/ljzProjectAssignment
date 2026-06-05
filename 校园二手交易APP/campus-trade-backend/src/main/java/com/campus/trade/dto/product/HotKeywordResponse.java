package com.campus.trade.dto.product;

import java.time.LocalDateTime;

public class HotKeywordResponse {

    private final String keyword;
    private final long searchCount;
    private final LocalDateTime lastSearchedAt;

    public HotKeywordResponse(String keyword, long searchCount, LocalDateTime lastSearchedAt) {
        this.keyword = keyword;
        this.searchCount = searchCount;
        this.lastSearchedAt = lastSearchedAt;
    }

    public String getKeyword() {
        return keyword;
    }

    public long getSearchCount() {
        return searchCount;
    }

    public LocalDateTime getLastSearchedAt() {
        return lastSearchedAt;
    }
}
