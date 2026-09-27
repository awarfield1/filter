package com.example.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The entry point for building filters programmatically.
 *
 * Usage:
 *
 * Filter adminsOver30 = and(
 *         equalTo("role", "administrator"),
 *         greaterThan("age", "30"));
 */
public final class Filters {

    private Filters() {
    }

    /** A filter matching every resource. */
    public static Filter alwaysTrue() {
        return BooleanLiteral.TRUE;
    }

    /** A filter matching no resource. */
    public static Filter alwaysFalse() {
        return BooleanLiteral.FALSE;
    }

    /** A boolean literal. */
    public static Filter literal(boolean value) {
        return BooleanLiteral.of(value);
    }

    /** Conjunction of the given operands. */
    public static Filter and(Filter... operands) {
        return and(Arrays.asList(operands));
    }

    /** Conjunction of the given operands; empty means {@link #alwaysTrue()}. */
    public static Filter and(List<Filter> operands) {
        List<Filter> copy = checkedCopy(operands);
        return switch (copy.size()) {
            case 0 -> BooleanLiteral.TRUE;
            case 1 -> copy.get(0);
            default -> new And(copy);
        };
    }

    /** Disjunction of the given operands. */
    public static Filter or(Filter... operands) {
        return or(Arrays.asList(operands));
    }

    /** Disjunction of the given operands; empty means {@link #alwaysFalse()}. */
    public static Filter or(List<Filter> operands) {
        List<Filter> copy = checkedCopy(operands);
        return switch (copy.size()) {
            case 0 -> BooleanLiteral.FALSE;
            case 1 -> copy.get(0);
            default -> new Or(copy);
        };
    }

    /** Negation of the given filter. */
    public static Filter not(Filter operand) {
        return new Not(operand);
    }

    /** Matches resources that have the named property. */
    public static Filter present(String property) {
        return new PropertyPresent(property);
    }

    /** Matches resources whose property equals {@code value} (case insensitively). */
    public static Filter equalTo(String property, String value) {
        return comparison(property, ComparisonOperator.EQUAL_TO, value);
    }

    /** Matches resources that have the property and whose value differs from {@code value}. */
    public static Filter notEqualTo(String property, String value) {
        return comparison(property, ComparisonOperator.NOT_EQUAL_TO, value);
    }

    /** Matches resources whose property is less than {@code value}. */
    public static Filter lessThan(String property, String value) {
        return comparison(property, ComparisonOperator.LESS_THAN, value);
    }

    /** Matches resources whose property is less than or equal to {@code value}. */
    public static Filter lessThanOrEqualTo(String property, String value) {
        return comparison(property, ComparisonOperator.LESS_THAN_OR_EQUAL_TO, value);
    }

    /** Matches resources whose property is greater than {@code value}. */
    public static Filter greaterThan(String property, String value) {
        return comparison(property, ComparisonOperator.GREATER_THAN, value);
    }

    /** Matches resources whose property is greater than or equal to {@code value}. */
    public static Filter greaterThanOrEqualTo(String property, String value) {
        return comparison(property, ComparisonOperator.GREATER_THAN_OR_EQUAL_TO, value);
    }

    /** Matches resources whose property value contains a match for {@code regex}. */
    public static Filter matchesRegex(String property, String regex) {
        return new PropertyMatchesRegex(property, regex);
    }

    /** Comparison factory that is useful when the operator is itself data. */
    public static Filter comparison(String property, ComparisonOperator operator, String value) {
        return new PropertyComparison(property, operator, value);
    }

    /**
     * Comparison with an explicit matching rule.
     *
     * Prefer {@link FilterSchema} when several properties have declared semantics.
     *
     * This overload is for one-off cases and for third-party rules.
     */
    public static Filter comparison(String property,
                                    ComparisonOperator operator,
                                    String value,
                                    MatchingRule matchingRule) {
        return new PropertyComparison(property, operator, value, matchingRule);
    }

    private static List<Filter> checkedCopy(List<Filter> operands) {
        Objects.requireNonNull(operands, "operands");
        List<Filter> copy = new ArrayList<>(operands.size());
        for (Filter operand : operands) {
            copy.add(Objects.requireNonNull(operand, "operand"));
        }
        return copy;
    }
}
