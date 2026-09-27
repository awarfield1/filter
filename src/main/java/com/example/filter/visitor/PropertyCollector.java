package com.example.filter.visitor;

import com.example.filter.And;
import com.example.filter.BooleanLiteral;
import com.example.filter.ExtensibleFilter;
import com.example.filter.Filter;
import com.example.filter.FilterVisitor;
import com.example.filter.Not;
import com.example.filter.Or;
import com.example.filter.PropertyComparison;
import com.example.filter.PropertyMatchesRegex;
import com.example.filter.PropertyPresent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A canonical visitor that collects the names of every property referenced by a tree
 * composed of core filter types.
 *
 * Example:
 *
 *  Set<String> referenced = PropertyCollector.collect(filter);
 *  if (!QUERYABLE.containsAll(referenced)) {
 *      throw new IllegalArgumentException("Filter references non-queryable properties");
 *  }
 *
 * This visitor does not interpret extension filters because their property references
 * cannot be inferred from the generic {@link ExtensibleFilter} contract.
 *
 * @throws UnsupportedFilterException if the tree contains an extension filter
 */
public final class PropertyCollector implements FilterVisitor<Void, Set<String>> {

    private static final PropertyCollector INSTANCE = new PropertyCollector();

    private PropertyCollector() {
    }

    /** Returns the property names referenced by the filter in the order they are encountered. */
    public static Set<String> collect(Filter filter) {
        Set<String> properties = new LinkedHashSet<>();
        filter.accept(INSTANCE, properties);
        return Collections.unmodifiableSet(properties);
    }

    @Override
    public Void visitBooleanLiteral(BooleanLiteral filter, Set<String> properties) {
        return null;
    }

    @Override
    public Void visitAnd(And filter, Set<String> properties) {
        return visitAll(filter.operands(), properties);
    }

    @Override
    public Void visitOr(Or filter, Set<String> properties) {
        return visitAll(filter.operands(), properties);
    }

    @Override
    public Void visitNot(Not filter, Set<String> properties) {
        return filter.operand().accept(this, properties);
    }

    @Override
    public Void visitPropertyPresent(PropertyPresent filter, Set<String> properties) {
        properties.add(filter.property());
        return null;
    }

    @Override
    public Void visitPropertyComparison(PropertyComparison filter, Set<String> properties) {
        properties.add(filter.property());
        return null;
    }

    @Override
    public Void visitPropertyMatchesRegex(PropertyMatchesRegex filter, Set<String> properties) {
        properties.add(filter.property());
        return null;
    }

    private Void visitAll(Iterable<Filter> operands, Set<String> properties) {
        for (Filter operand : operands) {
            operand.accept(this, properties);
        }
        return null;
    }
}
