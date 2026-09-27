package com.example.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatchingRuleTest {

    private static final Map<String, String> BUILD = Map.of("build", "10");
    private static final Map<String, String> USER =
            Map.of("firstname", "Joe", "role", "Administrator", "age", "35");

    @Test
    @DisplayName("AUTO infers numbers from the data, which is convenient but data-dependent")
    void autoComparesNumericallyWhenBothSidesLookNumeric() {
        assertTrue(Filters.greaterThan("age", "30").matches(USER));
        // Both sides look numeric, so 10 > 9.
        assertTrue(Filters.greaterThan("build", "9").matches(BUILD));
    }

    @Test
    @DisplayName("a declared text rule orders by text, which is the opposite answer here")
    void caseIgnoreComparesLexically() {
        FilterSchema schema = FilterSchema.builder().caseIgnore("build").build();

        // Read as text, "10" sorts before "9". The opposite of AUTO. That is the point of
        // declaring: the answer no longer depends on what the value happens to look like.
        assertFalse(schema.greaterThan("build", "9").matches(BUILD));
        assertTrue(schema.lessThan("build", "9").matches(BUILD));
    }

    @Test
    @DisplayName("values are case insensitive by default and case sensitive when declared")
    void caseSensitivityIsDeclarable() {
        assertTrue(Filters.equalTo("role", "administrator").matches(USER));

        FilterSchema caseSensitive = FilterSchema.builder().caseExact("role").build();
        assertFalse(caseSensitive.equalTo("role", "administrator").matches(USER));
        assertTrue(caseSensitive.equalTo("role", "Administrator").matches(USER));
    }

    @Test
    @DisplayName("a numeric property compares numerically regardless of the stored text")
    void numericIgnoresTextOrdering() {
        FilterSchema schema = FilterSchema.builder().numeric("age").build();
        Map<String, String> child = Map.of("age", "9");

        assertFalse(schema.greaterThan("age", "30").matches(child));
        assertTrue(schema.lessThan("age", "30").matches(child));
    }

    @Test
    @DisplayName("a value that violates its declared rule simply does not match")
    void nonNumericStoredValuesDoNotMatchANumericRule() {
        FilterSchema schema = FilterSchema.builder().numeric("age").build();
        Map<String, String> corrupt = Map.of("age", "unknown");

        assertFalse(schema.greaterThan("age", "30").matches(corrupt));
        assertFalse(schema.lessThan("age", "30").matches(corrupt));
    }

    @Test
    @DisplayName("an invalid operand fails when the filter is built, not silently at query time")
    void invalidOperandsAreRejectedEagerly() {
        FilterSchema schema = FilterSchema.builder().numeric("age").build();

        InvalidOperandException failure = assertThrows(InvalidOperandException.class,
                () -> schema.greaterThan("age", "not-a-number"));

        assertEquals("not-a-number", failure.getOperand());
        assertEquals("NUMERIC", failure.getRule());
    }

    @Test
    @DisplayName("parsing against a schema yields the same filter as building against it")
    void parsingAndBuildingAgree() {
        FilterSchema schema = FilterSchema.builder().numeric("age").build();

        assertEquals(schema.greaterThan("age", "30"), Filter.parse("(age>30)", schema));
        // Without a schema, age uses AUTO matching instead of NUMERIC matching
        assertFalse(Filter.parse("(age>30)").equals(Filter.parse("(age>30)", schema)));
    }

    @Test
    @DisplayName("the string form is unchanged; the rule is recovered by parsing with the schema")
    void matchingRulesDoNotLeakIntoTheStringForm() {
        FilterSchema schema = FilterSchema.builder().numeric("age").build();
        Filter filter = schema.greaterThan("age", "30");

        assertEquals("(age>30)", filter.toFilterString());
        assertEquals(filter, Filter.parse(filter.toFilterString(), schema));
    }

    @Test
    @DisplayName("third parties can supply their own rule")
    void customRulesAreSupported() {
        MatchingRule lengthOrder = new MatchingRule() {
            @Override
            public String name() {
                return "lengthOrder";
            }

            @Override
            public Integer compare(String propertyValue, String operand) {
                return Integer.compare(propertyValue.length(), operand.length());
            }
        };

        FilterSchema schema = FilterSchema.builder().property("firstname", lengthOrder).build();

        assertTrue(schema.greaterThan("firstname", "Al").matches(USER));
        assertFalse(schema.greaterThan("firstname", "Anna").matches(USER));
    }

    @Test
    @DisplayName("undeclared properties keep the default rule")
    void undeclaredPropertiesUseTheDefaultRule() {
        FilterSchema schema = FilterSchema.builder()
                .numeric("age")
                .defaultRule(MatchingRules.CASE_EXACT)
                .build();

        assertEquals(MatchingRules.NUMERIC, schema.ruleFor("age"));
        assertEquals(MatchingRules.CASE_EXACT, schema.ruleFor("anything-else"));
    }
}
