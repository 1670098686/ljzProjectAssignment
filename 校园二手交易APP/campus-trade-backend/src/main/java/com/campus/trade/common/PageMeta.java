package com.campus.trade.common;

public class PageMeta {

    private int page;
    private int size;
    private long total;

    public PageMeta() {
    }

    public PageMeta(int page, int size, long total) {
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
