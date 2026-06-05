package com.campus.trade.dto.product;

import java.util.ArrayList;
import java.util.List;

public class SearchSuggestionResponse {

    private String keyword;
    private boolean hasMore;
    private List<String> suggestions = new ArrayList<>();

    public SearchSuggestionResponse() {
    }

    public SearchSuggestionResponse(String keyword, List<String> suggestions, boolean hasMore) {
        this.keyword = keyword;
        this.suggestions = suggestions;
        this.hasMore = hasMore;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
