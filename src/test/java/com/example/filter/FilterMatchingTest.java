package com.example.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.example.filter.Filters.alwaysFalse;
import static com.example.filter.Filters.alwaysTrue;
import static com.example.filter.Filters.and;
import static com.example.filter.Filters.equalTo;
import static com.example.filter.Filters.greaterThan;
import static com.example.filter.Filters.lessThan;
import static com.example.filter.Filters.matchesRegex;
import static com.example.filter.Filters.not;
import static com.example.filter.Filters.notEqualTo;
import static com.example.filter.Filters.or;
import static com.example.filter.Filters.present;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FilterMatchingTest {

    private static Map<String, String> user(String age) {
        Map<String, String> user = new LinkedHashMap<>();
        user.put("firstname", "Joe");
        user.put("surname", "Bloggs");
        user.put("role", "administrator");
        user.put("age", age);
        return user;
    }

    @Test
    @DisplayName("the scenario from the specification")
    void specificationScenario() {
        Map<String, String> user = user("35");

        Filter filter = and(
                equalTo("role", "administrator"),
                greaterThan("age", "30"));

        assertTrue(filter.matches(user));

        user.put("age", "25");
        assertFalse(filter.matches(user));
    }

    @Nested
    class BooleanLiterals {

        @Test
        void trueMatchesEverythingAndFalseNothing() {
            assertTrue(alwaysTrue().matches(Map.of()));
            assertFalse(alwaysFalse().matches(Map.of()));
            assertTrue(alwaysTrue().matches(user("35")));
            assertFalse(alwaysFalse().matches(user("35")));
        }
    }

    @Nested
    class LogicalOperators {

        @Test
        void andRequiresEveryOperand() {
            assertTrue(and(alwaysTrue(), alwaysTrue()).matches(Map.of()));
            assertFalse(and(alwaysTrue(), alwaysFalse()).matches(Map.of()));
        }

        @Test
        void orRequiresOneOperand() {
            assertTrue(or(alwaysFalse(), alwaysTrue()).matches(Map.of()));
            assertFalse(or(alwaysFalse(), alwaysFalse()).matches(Map.of()));
        }

        @Test
        void notInverts() {
            assertTrue(not(alwaysFalse()).matches(Map.of()));
            assertFalse(not(alwaysTrue()).matches(Map.of()));
        }

        @Test
        void emptyCompositesCollapseToTheirIdentityElement() {
            assertEquals(BooleanLiteral.TRUE, and());
            assertEquals(BooleanLiteral.FALSE, or());
        }

        @Test
        void singleOperandCompositesCollapseToThatOperand() {
            Filter operand = equalTo("role", "administrator");
            assertEquals(operand, and(operand));
            assertEquals(operand, or(operand));
        }

        @Test
        void deeplyNestedFiltersCanBeComposed() {
            Filter filter = or(
                    and(equalTo("role", "administrator"), greaterThan("age", "30")),
                    and(equalTo("role", "auditor"), not(present("suspended"))));

            assertTrue(filter.matches(user("35")));
            assertFalse(filter.matches(user("25")));
            assertTrue(filter.matches(Map.of("role", "auditor")));
            assertFalse(filter.matches(Map.of("role", "auditor", "suspended", "true")));
        }

        @Test
        void fluentCompositionIsEquivalentToTheFactories() {
            assertEquals(
                    and(equalTo("a", "1"), equalTo("b", "2")),
                    equalTo("a", "1").and(equalTo("b", "2")));
            assertEquals(not(equalTo("a", "1")), equalTo("a", "1").negate());
        }
    }

    @Nested
    class Presence {

        @Test
        void detectsPresentAndMissingProperties() {
            assertTrue(present("role").matches(user("35")));
            assertFalse(present("email").matches(user("35")));
        }

        @Test
        void propertyNamesAreCaseSensitive() {
            assertTrue(present("role").matches(user("35")));
            assertFalse(present("Role").matches(user("35")));
        }
    }

    @Nested
    class Comparisons {

        @Test
        void equalityIgnoresValueCase() {
            assertTrue(equalTo("role", "ADMINISTRATOR").matches(user("35")));
            assertTrue(equalTo("role", "administrator").matches(user("35")));
        }

        @Test
        void numericValuesAreComparedNumericallyNotLexically() {
            Map<String, String> user = user("100");
            assertTrue(greaterThan("age", "30").matches(user));
            assertFalse(lessThan("age", "30").matches(user));
        }

        @Test
        void nonNumericValuesAreComparedLexically() {
            assertTrue(lessThan("surname", "Smith").matches(user("35")));
            assertTrue(greaterThan("surname", "Adams").matches(user("35")));
        }

        @Test
        void everyComparisonAgainstAMissingPropertyIsFalse() {
            Map<String, String> user = user("35");
            for (ComparisonOperator operator : ComparisonOperator.values()) {
                assertFalse(Filters.comparison("email", operator, "anything").matches(user),
                        () -> operator + " should not match a missing property");
            }
        }

        @Test
        @DisplayName("notEqualTo requires the property to exist, whereas not(equalTo) does not")
        void negationSemanticsAreDistinct() {
            Map<String, String> user = user("35");

            assertFalse(notEqualTo("email", "joe@example.com").matches(user));
            assertTrue(not(equalTo("email", "joe@example.com")).matches(user));
        }
    }

    @Nested
    class RegularExpressions {

        @Test
        void matchesAreUnanchoredAndCaseInsensitive() {
            Map<String, String> user = user("35");
            assertTrue(matchesRegex("surname", "logg").matches(user));
            assertTrue(matchesRegex("surname", "BLOGGS").matches(user));
            assertTrue(matchesRegex("surname", "^Bl.*gs$").matches(user));
            assertFalse(matchesRegex("surname", "^logg").matches(user));
        }

        @Test
        void missingPropertiesNeverMatch() {
            assertFalse(matchesRegex("email", ".*").matches(user("35")));
        }
    }

    @Nested
    class Validation {

        @Test
        void reservedCharactersAreRejectedInPropertyNames() {
            assertThrows(IllegalArgumentException.class, () -> present("ro(le"));
            assertThrows(IllegalArgumentException.class, () -> equalTo("ro=le", "x"));
            assertThrows(IllegalArgumentException.class, () -> present(""));
        }

        @Test
        void nullsAreRejectedEagerly() {
            assertThrows(NullPointerException.class, () -> equalTo("role", null));
            assertThrows(NullPointerException.class, () -> not(null));
        }
    }
}
