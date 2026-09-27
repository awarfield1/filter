package com.example.filter.visitor;

import com.example.filter.ComparisonOperator;
import com.example.filter.ExtensibleFilter;
import com.example.filter.Filter;
import com.example.filter.MatchingRules;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.example.filter.Filters.alwaysFalse;
import static com.example.filter.Filters.alwaysTrue;
import static com.example.filter.Filters.and;
import static com.example.filter.Filters.comparison;
import static com.example.filter.Filters.equalTo;
import static com.example.filter.Filters.greaterThan;
import static com.example.filter.Filters.matchesRegex;
import static com.example.filter.Filters.not;
import static com.example.filter.Filters.notEqualTo;
import static com.example.filter.Filters.or;
import static com.example.filter.Filters.present;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterDescriberTest {

    @Test
    @DisplayName("literals read as words")
    void describesLiterals() {
        assertEquals("anything", FilterDescriber.describe(alwaysTrue()));
        assertEquals("nothing", FilterDescriber.describe(alwaysFalse()));
    }

    @Test
    @DisplayName("leaf filters read as clauses")
    void describesLeaves() {
        assertEquals("\"email\" is present", FilterDescriber.describe(present("email")));
        assertEquals("\"role\" is equal to \"admin\"", FilterDescriber.describe(equalTo("role", "admin")));
        assertEquals("\"age\" is greater than \"30\"", FilterDescriber.describe(greaterThan("age", "30")));
        assertEquals("\"surname\" matches /^bl/", FilterDescriber.describe(matchesRegex("surname", "^bl")));
    }

    /** A comparison against a missing property is false, so "not equal to" also requires the property to exist. */
    @Test
    @DisplayName("not-equal-to states that the property must be present")
    void describesNotEqualToPrecisely() {
        assertEquals("\"role\" is present and not equal to \"admin\"",
                FilterDescriber.describe(notEqualTo("role", "admin")));
    }

    @Test
    @DisplayName("conjunctions and disjunctions read as sentences")
    void describesBooleanGroups() {
        assertEquals("\"role\" is equal to \"admin\" and \"age\" is greater than \"30\"",
                FilterDescriber.describe(and(equalTo("role", "admin"), greaterThan("age", "30"))));
        assertEquals("\"role\" is equal to \"admin\" or \"role\" is equal to \"auditor\"",
                FilterDescriber.describe(or(equalTo("role", "admin"), equalTo("role", "auditor"))));
    }

    @Test
    @DisplayName("negation is parenthesised")
    void describesNegation() {
        assertEquals("not (\"email\" is present)", FilterDescriber.describe(not(present("email"))));
    }

    @Test
    @DisplayName("nested groups are parenthesised so the reading is unambiguous")
    void parenthesisesNestedGroups() {
        Filter filter = and(
                or(equalTo("role", "admin"), equalTo("role", "auditor")),
                present("email"));

        assertEquals("(\"role\" is equal to \"admin\" or \"role\" is equal to \"auditor\") "
                        + "and \"email\" is present",
                FilterDescriber.describe(filter));
    }

    @Test
    @DisplayName("a non-default matching rule is named")
    void mentionsExplicitMatchingRules() {
        Filter filter = comparison("id", ComparisonOperator.EQUAL_TO, "AbC", MatchingRules.CASE_EXACT);

        assertEquals("\"id\" is equal to \"AbC\" (case_exact)", FilterDescriber.describe(filter));
    }

    @Test
    @DisplayName("unrecognised extensions are described generically rather than rejected")
    void describesUnknownExtensions() {
        assertEquals("extension example.com:filter:near with property=\"office\"",
                FilterDescriber.describe(new Near("office")));
    }

    @Test
    @DisplayName("unrecognised extensions with operands are described generically rather than rejected")
    void describesUnknownExtensionsWithOperands() {
        assertEquals("extension example.com:filter:ExtensionWithOperands with property=\"office\"; " +
                        "operands: (\"role\" is equal to \"admin\", \"email\" is present)",
                FilterDescriber.describe(new ExtensionWithOperands("office", List.of(equalTo("role", "admin"), present("email")))));
    }

    // region start example extensions

    /** A parameterised extension with no nested filters. */
    record Near(String property) implements ExtensibleFilter {

        @Override
        public String extensionId() {
            return "example.com:filter:near";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("property", property);
        }

        @Override
        public List<Filter> operands() {
            return List.of();
        }

        @Override
        public boolean matches(Map<String, String> resource) {
            return resource.containsKey(property);
        }

        @Override
        public String toFilterString() {
            return "(near:" + property + ")";
        }
    }

    /** A parameterised extension with nested filters. */
    record ExtensionWithOperands(String property, List<Filter> operands) implements ExtensibleFilter {

        @Override
        public String extensionId() {
            return "example.com:filter:ExtensionWithOperands";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("property", property);
        }

        @Override
        public List<Filter> operands() {
            return operands;
        }

        @Override
        public boolean matches(Map<String, String> resource) {
            return false;
        }

        @Override
        public String toFilterString() {
            return "";
        }
    }

    // region end example extensions
}
