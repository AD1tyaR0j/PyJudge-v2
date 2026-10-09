package com.pyjudge.model;

import java.util.List;

/** One page of a longer result list. */
public class Page<T> {
    private final List<T> items;
    private final int page;
    private final int perPage;
    private final long total;

    public Page(List<T> items, int page, int perPage, long total) {
        this.items = items;
        this.page = page;
        this.perPage = perPage;
        this.total = total;
    }

    public List<T> getItems() { return items; }
    public int getPage() { return page; }
    public long getTotal() { return total; }

    public int getPages() {
        return (int) Math.max(1, (total + perPage - 1) / perPage);
    }

    public boolean isHasPrev() { return page > 1; }
    public boolean isHasNext() { return page < getPages(); }
    public int getPrevNum() { return page - 1; }
    public int getNextNum() { return page + 1; }
}
