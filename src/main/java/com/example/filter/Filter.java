package com.example.filter;

import java.util.Map;

/**
 * An immutable, composable predicate over a resource.
 *
 * A resource is a map of property names to values.
 *
 * Property names are case-sensitive. Property values are compared case-insensitively.
 *
 * Filters are composed into trees using {@link Filters#and}, {@link Filters#or}
 * and {@link Filters#not}.
 *
 * Consume a filter directly with {@link #matches(Map)} or expose its structure to a visitor with
 * {@link #accept(FilterVisitor, Object)}.
 *
 * The interface is deliberately not sealed: New filter types can be added simply by implementing it.
 *
 * @see Filters for the factory methods used to build filters
 */
public interface Filter {

    /**
     * Returns {@code true} if the given resource satisfies this filter.
     *
     * @param resource the resource to test; never {@code null}. A property that is not
     *                 present in the map is treated as missing instead of null-valued.
     */
    boolean matches(Map<String, String> resource);

    /**
     * Dispatches to the visitor method matching this filter's concrete type.
     *
     * @param visitor   the visitor to dispatch to
     * @param parameter the context to pass through the traversal
     * @param <R>       the type produced by the visitor
     * @param <P>       the type of the traversal context
     */
    <R, P> R accept(FilterVisitor<R, P> visitor, P parameter);

    /**
     * Returns the string representation of this filter.
     *
     * For core filter types, the representation is canonical and can be
     * parsed back using {@link #parse(String)}.
     *
     * Extension filters are not automatically understood by
     * {@link FilterParser}. An extension may choose to render itself
     * as an equivalent core filter or as an extension-specific representation.
     *
     * The syntax is an LDAP-style prefix notation (see {@link FilterParser}).
     */
    String toFilterString();

    /** Returns a filter matching resources that match both this filter and {@code other}. */
    default Filter and(Filter other) {
        return Filters.and(this, other);
    }

    /** Returns a filter matching resources that match either this filter or {@code other}. */
    default Filter or(Filter other) {
        return Filters.or(this, other);
    }

    /** Returns a filter matching exactly the resources this filter does not match. */
    default Filter negate() {
        return Filters.not(this);
    }

    /**
     * Parses the canonical string representation produced by {@link #toFilterString()},
     *
     * @throws FilterParseException      if the input is not a well-formed filter string
     */
    static Filter parse(String filterString) {
        return FilterParser.parse(filterString);
    }


    /**
     * Parses the canonical string representation, resolving matching rules from a schema.
     *
     * @see FilterSchema
     */
    static Filter parse(String filterString, FilterSchema schema) {
        return FilterParser.parse(filterString, schema);
    }

}
