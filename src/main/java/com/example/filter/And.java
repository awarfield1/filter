package com.example.filter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Logical conjunction: matches when every operand matches.
 *
 * String form: {@code (&(a=1)(b=2))}.
 *
 * @param operands the operands; never empty
 */
public record And(List<Filter> operands) implements Filter {

    public And {
        Objects.requireNonNull(operands, "operands");
        operands = List.copyOf(operands);
        if (operands.isEmpty()) {
            throw new IllegalArgumentException("AND requires at least one operand; use BooleanLiteral.TRUE instead");
        }
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        for (Filter operand : operands) {
            if (!operand.matches(resource)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitAnd(this, parameter);
    }

    @Override
    public String toFilterString() {
        return FilterStrings.composite('&', operands);
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
