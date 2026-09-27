package com.example.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtensibilityTest {

    private static final Map<String, String> JOE =
            Map.of("firstname", "Joe",
                    "role", "administrator",
                    "age", "35");

    @Test
    @DisplayName("5a: a new filter type is usable everywhere a core filter is")
    void customFilterTypesComposeWithCoreTypes() {
        Filter filter = Filters.and(
                new OneOf("role", List.of("administrator", "auditor")),
                Filters.greaterThan("age", "30"));

        assertTrue(filter.matches(JOE));
        assertFalse(filter.matches(Map.of("role", "guest", "age", "35")));
    }

    @Test
    @DisplayName("5b: a visitor rejects unknown types loudly rather than silently ignoring them")
    void unknownExtensionsAreRejectedByDefault() {
        Filter filter = new OneOf("role", List.of("administrator"));

        UnsupportedFilterException failure = assertThrows(UnsupportedFilterException.class, () -> new CountingVisitor().count(filter));

        assertSame(filter, failure.getFilter());
    }

    @Test
    @DisplayName("a visitor needs no mutable state")
    void visitorsCanBeStatelessSingletons() {
        Filter first = Filters.and(Filters.equalTo("role", "administrator"), Filters.present("email"));
        Filter second = Filters.not(Filters.greaterThan("age", "30"));

        CountingVisitor shared = new CountingVisitor();

        assertEquals(3, shared.count(first));
        assertEquals(2, shared.count(second));
        assertEquals(3, shared.count(first));
    }

    // region start example filter and visitor

    /**
     * An example filter type contributed by a third party.
     */
    record OneOf(String property, List<String> values) implements ExtensibleFilter {

        @Override
        public String extensionId() {
            return "example.com:filter:oneOf";
        }

        @Override
        public Map<String, String> parameters() {
            return Map.of("property", property, "values", String.join(",", values));
        }

        @Override
        public boolean matches(Map<String, String> resource) {
            String actual = resource.get(property);
            return actual != null && values.stream().anyMatch(actual::equalsIgnoreCase);
        }

        @Override
        public String toFilterString() {
            return "(" + property + "~=^(" + String.join("|", values) + ")$)";
        }
    }

    /** Counts nodes, accumulating through the parameter int[] rather than a field. */
    static final class CountingVisitor implements FilterVisitor<Void, int[]> {

        int count(Filter filter) {
            int[] total = new int[1];
            filter.accept(this, total);
            return total[0];
        }

        @Override
        public Void visitBooleanLiteral(BooleanLiteral filter, int[] total) {
            total[0]++;
            return null;
        }

        @Override
        public Void visitAnd(And filter, int[] total) {
            total[0]++;
            filter.operands().forEach(operand -> operand.accept(this, total));
            return null;
        }

        @Override
        public Void visitOr(Or filter, int[] total) {
            total[0]++;
            filter.operands().forEach(operand -> operand.accept(this, total));
            return null;
        }

        @Override
        public Void visitNot(Not filter, int[] total) {
            total[0]++;
            return filter.operand().accept(this, total);
        }

        @Override
        public Void visitPropertyPresent(PropertyPresent filter, int[] total) {
            total[0]++;
            return null;
        }

        @Override
        public Void visitPropertyComparison(PropertyComparison filter, int[] total) {
            total[0]++;
            return null;
        }

        @Override
        public Void visitPropertyMatchesRegex(PropertyMatchesRegex filter, int[] total) {
            total[0]++;
            return null;
        }

        // region end example filter and visitor
    }
}
