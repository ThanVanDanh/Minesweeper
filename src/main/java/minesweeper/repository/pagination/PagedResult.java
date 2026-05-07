package minesweeper.repository.pagination;

import java.util.List;

/**
 * A page of results from a paginated query
 */
public class PagedResult<T> {
    private final List<T> content;
    private final Page pageInfo;

    public PagedResult(List<T> content, Page pageInfo) {
        this.content = content;
        this.pageInfo = pageInfo;
    }

    public List<T> getContent() {
        return content;
    }

    public Page getPageInfo() {
        return pageInfo;
    }

    public int getPageNumber() {
        return pageInfo.getPageNumber();
    }

    public int getPageSize() {
        return pageInfo.getPageSize();
    }

    public long getTotalElements() {
        return pageInfo.getTotalElements();
    }

    public int getTotalPages() {
        return pageInfo.getTotalPages();
    }

    public boolean hasNext() {
        return pageInfo.hasNext();
    }

    public boolean hasPrevious() {
        return pageInfo.hasPrevious();
    }

    @Override
    public String toString() {
        return String.format("PagedResult{%s, items=%d}", pageInfo, content.size());
    }
}
