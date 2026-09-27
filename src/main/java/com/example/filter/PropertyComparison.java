package com.example.filter;

import java.util.Map;
import java.util.Objects;

/**
 * Compares a property value with a fixed operand using a {@link ComparisonOperator}.
 *
 * A comparison against a property that is absent from the resource is always
 * false, including for {@link ComparisonOperator#NOT_EQUAL_TO}. This treats
 * missing properties consistently and avoids giving special meaning to
 * {@code NOT_EQUAL_TO}.
 *
 * Example:
 *
 * To express "the resource does not have the role administrator", whether or not
 * it has a role at all, negate the equality:
 *
 * {@code Filters.not(Filters.equalTo("role", "administrator"))}.
 *
 * Values are compared using the {@link MatchingRule} carried by this comparison,
 * which defaults to {@link MatchingRules#AUTO}.
 *
 * String form:
 *
 *  {@code (age>30)}
 *  {@code (role=administrator)}
 *  {@code (role!=guest)}
 *
 * @param property     the (case-sensitive) property name
 * @param operator     the relational operator to apply
 * @param value        the operand to compare the property value against
 * @param matchingRule how values of this property are compared
 */
public record PropertyComparison(String property,
                                 ComparisonOperator operator,
                                 String value,
                                 MatchingRule matchingRule) implements Filter {

    public PropertyComparison {
        PropertyNames.validate(property);
        Objects.requireNonNull(operator, "operator");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(matchingRule, "matchingRule");
        matchingRule.validateOperand(value);
    }

    /** Creates a comparison using {@link MatchingRules#AUTO}. */
    public PropertyComparison(String property, ComparisonOperator operator, String value) {
        this(property, operator, value, MatchingRules.AUTO);
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource");
        String actual = resource.get(property);
        if (actual == null) {
            return false;
        }
        Integer comparison = matchingRule.compare(actual, value);
        return comparison != null && operator.test(comparison);
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitPropertyComparison(this, parameter);
    }

    @Override
    public String toFilterString() {
        return "(" + property + operator.symbol() + FilterStrings.escape(value) + ")";
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
