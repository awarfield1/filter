package com.example.filter;

import java.util.Map;
import java.util.Objects;

/**
 * Logical negation: matches exactly the resources the operand does not match.
 *
 * Negation is not always equivalent to a negated comparison.
 * Missing properties are treated differently from properties that are present but fail a comparison.
 *
 * Example:
 *
 * A resource without a "role" property matches (!(role=admin)) because (role=admin) is false and we negate that.
 *
 * A resource without a "role" property does not match (role!=admin) because "role" is absent.
 *
 * @param operand the filter to negate
 */
public record Not(Filter operand) implements Filter {

    public Not {
        Objects.requireNonNull(operand, "operand");
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        return !operand.matches(resource);
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitNot(this, parameter);
    }

    @Override
    public String toFilterString() {
        return "(!" + operand.toFilterString() + ")";
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
