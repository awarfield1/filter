package com.example.filter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Logical disjunction: matches when at least one operand matches.
 *
 * Operands are evaluated left-to-right. Evaluation short-circuits on the first matching operand.
 *
 * String form: {@code (|(a=1)(b=2))}.
 *
 * @param operands the operands
 */
public record Or(List<Filter> operands) implements Filter {

    public Or {
        Objects.requireNonNull(operands, "operands");
        operands = List.copyOf(operands);
        if (operands.isEmpty()) {
            throw new IllegalArgumentException("OR requires at least one operand; use BooleanLiteral.FALSE instead");
        }
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        for (Filter operand : operands) {
            if (operand.matches(resource)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitOr(this, parameter);
    }

    @Override
    public String toFilterString() {
        return FilterStrings.composite('|', operands);
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
