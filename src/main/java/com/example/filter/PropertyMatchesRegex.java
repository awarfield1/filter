package com.example.filter;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Matches resources whose property value contains a match for a regular expression.
 *
 * Semantics:
 *
 * - Matching is unanchored ({@link java.util.regex.Matcher#find()}).
 * - Use ^ and $ for a whole-value match.
 * - Matching is case insensitive.
 * - A missing property never matches.
 *
 * String form: {@code (firstname~=^jo)}.
 *
 * This is a class rather than a record so that the {@link Pattern} can be
 * compiled once at construction time while equality remains based on the
 * property name and regex source.
 */
public final class PropertyMatchesRegex implements Filter {

    private final String property;
    private final String regex;
    private final Pattern pattern;

    /**
     * @param property the (case sensitive) property name
     * @param regex    the regular expression, in {@link Pattern} syntax
     * @throws PatternSyntaxException if {@code regex} is not a valid regular expression
     */
    public PropertyMatchesRegex(String property, String regex) {
        PropertyNames.validate(property);
        this.property = property;
        this.regex = Objects.requireNonNull(regex, "regex");
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /** The (case sensitive) property name. */
    public String property() {
        return property;
    }

    /** The regular expression source, as supplied. */
    public String regex() {
        return regex;
    }

    /** The compiled, case-insensitive pattern. */
    public Pattern pattern() {
        return pattern;
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource");
        String actual = resource.get(property);
        return actual != null && pattern.matcher(actual).find();
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitPropertyMatchesRegex(this, parameter);
    }

    @Override
    public String toFilterString() {
        return "(" + property + "~=" + FilterStrings.escape(regex) + ")";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PropertyMatchesRegex that
                && property.equals(that.property)
                && regex.equals(that.regex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property, regex);
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
