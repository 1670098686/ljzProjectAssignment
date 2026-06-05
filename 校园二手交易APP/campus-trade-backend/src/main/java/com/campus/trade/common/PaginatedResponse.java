package com.campus.trade.common;

import java.util.List;

public class PaginatedResponse<T> {

    private List<T> items;
    private PageMeta meta;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> items, PageMeta meta) {
        this.items = items;
        this.meta = meta;
    }

    public static <T> PaginatedResponse<T> of(List<T> items, int page, int size, long total) {
        return new PaginatedResponse<>(items, new PageMeta(page, size, total));
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public PageMeta getMeta() {
        return meta;
    }

    public void setMeta(PageMeta meta) {
        this.meta = meta;
    }
}
