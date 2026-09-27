package com.example.filter;

/** Thrown when a {@link FilterVisitor} encounters a filter type it does not support. */
public class UnsupportedFilterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient Filter filter;

    public UnsupportedFilterException(Filter filter) {
        super("Unsupported filter type: " + (filter == null ? "null" : filter.getClass().getName()));
        this.filter = filter;
    }

    public UnsupportedFilterException(String message) {
        super(message);
        this.filter = null;
    }

    public Filter getFilter() {
        return filter;
    }
}
