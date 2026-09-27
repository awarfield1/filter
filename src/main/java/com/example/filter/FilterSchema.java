package com.example.filter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Associates resource properties with the {@link MatchingRule} used to compare their values.
 *
 * A schema provides a central place to declare numeric, lexicographic, and case-sensitivity rules.
 * Filters created through a schema carry the resolved rule with them so that comparison semantics are consistent.
 *
 * Example usage:
 *
 * Define a schema:
 *
 *      FilterSchema schema = FilterSchema.builder()
 *              .numeric("age")
 *              .caseExact("identifier")
 *              .build();
 *
 * Create a filter programmatically using the schema:
 *
 *      Filter olderThanNine = schema.greaterThan("age", "9");
 *
 * Create an equivalent filter by parsing a string with the schema:
 *
 *      Filter parsed = Filter.parse("(age>9)", schema);
 *
 * In both cases, "age" is compared as a number.
 *
 * Important: Undeclared properties fall back to {@link MatchingRules#AUTO} unless a different
 * default is configured, so a schema is entirely optional and can be introduced property
 * by property.
 */
public final class FilterSchema {

    /** A schema declaring nothing. Every property uses {@link MatchingRules#AUTO}. */
    public static final FilterSchema DEFAULT = builder().build();

    private final Map<String, MatchingRule> rules;
    private final MatchingRule defaultRule;

    private FilterSchema(Map<String, MatchingRule> rules, MatchingRule defaultRule) {
        this.rules = Collections.unmodifiableMap(new LinkedHashMap<>(rules));
        this.defaultRule = defaultRule;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** The rule governing {@code property}, or the default rule if it is undeclared. */
    public MatchingRule ruleFor(String property) {
        Objects.requireNonNull(property, "property");
        return rules.getOrDefault(property, defaultRule);
    }

    /** The explicitly declared property names, in declaration order. */
    public Set<String> declaredProperties() {
        return rules.keySet();
    }

    /** The rule applied to properties this schema does not declare. */
    public MatchingRule defaultRule() {
        return defaultRule;
    }

    /** Builds a comparison whose matching rule is taken from this schema. */
    public Filter comparison(String property, ComparisonOperator operator, String value) {
        return new PropertyComparison(property, operator, value, ruleFor(property));
    }

    /** Equality, under this schema's rule for the property. */
    public Filter equalTo(String property, String value) {
        return comparison(property, ComparisonOperator.EQUAL_TO, value);
    }

    /** Inequality, under this schema's rule for the property. */
    public Filter notEqualTo(String property, String value) {
        return comparison(property, ComparisonOperator.NOT_EQUAL_TO, value);
    }

    /** Strict ordering, under this schema's rule for the property. */
    public Filter lessThan(String property, String value) {
        return comparison(property, ComparisonOperator.LESS_THAN, value);
    }

    /** Ordering, under this schema's rule for the property. */
    public Filter lessThanOrEqualTo(String property, String value) {
        return comparison(property, ComparisonOperator.LESS_THAN_OR_EQUAL_TO, value);
    }

    /** Strict ordering, under this schema's rule for the property. */
    public Filter greaterThan(String property, String value) {
        return comparison(property, ComparisonOperator.GREATER_THAN, value);
    }

    /** Ordering, under this schema's rule for the property. */
    public Filter greaterThanOrEqualTo(String property, String value) {
        return comparison(property, ComparisonOperator.GREATER_THAN_OR_EQUAL_TO, value);
    }

    @Override
    public String toString() {
        return "FilterSchema" + rules + "default=" + defaultRule.name();
    }

    public static final class Builder {

        private final Map<String, MatchingRule> rules = new LinkedHashMap<>();
        private MatchingRule defaultRule = MatchingRules.AUTO;

        private Builder() {
        }

        /** Declares a property with an arbitrary rule, including a custom implementation. */
        public Builder property(String property, MatchingRule rule) {
            PropertyNames.validate(property);
            Objects.requireNonNull(rule, "rule");
            if (rules.putIfAbsent(property, rule) != null) {
                throw new IllegalArgumentException("Property already declared: " + property);
            }
            return this;
        }

        /** Declares a property compared as a decimal number. */
        public Builder numeric(String property) {
            return property(property, MatchingRules.NUMERIC);
        }

        /** Declares a property compared as case-insensitive text. */
        public Builder caseIgnore(String property) {
            return property(property, MatchingRules.CASE_IGNORE);
        }

        /** Declares a property compared as case-sensitive text. */
        public Builder caseExact(String property) {
            return property(property, MatchingRules.CASE_EXACT);
        }

        /** Sets the rule used for properties this schema does not declare. */
        public Builder defaultRule(MatchingRule rule) {
            this.defaultRule = Objects.requireNonNull(rule, "rule");
            return this;
        }

        public FilterSchema build() {
            return new FilterSchema(rules, defaultRule);
        }
    }
}
