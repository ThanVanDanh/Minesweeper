package minesweeper.repository.pagination;

/**
 * Pagination support for database queries
 */
public class Page {
    private final int pageNumber;    // 0-indexed
    private final int pageSize;
    private final long totalElements;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public Page(int pageNumber, int pageSize, long totalElements) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.hasNext = (long) (pageNumber + 1) * pageSize < totalElements;
        this.hasPrevious = pageNumber > 0;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return (int) Math.ceil((double) totalElements / pageSize);
    }

    public int getOffset() {
        return pageNumber * pageSize;
    }

    public int getLimit() {
        return pageSize;
    }

    public boolean hasNext() {
        return hasNext;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public String getLimitClause() {
        return " LIMIT " + pageSize + " OFFSET " + getOffset();
    }

    @Override
    public String toString() {
        return String.format("Page{%d/%d, size=%d, total=%d}", 
                pageNumber + 1, getTotalPages(), pageSize, totalElements);
    }
}
