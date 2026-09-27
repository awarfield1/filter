package com.example.filter;

import java.util.Map;

/**
 * The boolean literals {@code true} and {@code false}.
 *
 * {@link #TRUE} matches every resource, {@link #FALSE} matches none.
 *
 * String form: {@code (true)} and {@code (false)}.
 */
public enum BooleanLiteral implements Filter {

    TRUE(true),
    FALSE(false);

    private final boolean value;

    BooleanLiteral(boolean value) {
        this.value = value;
    }

    public static BooleanLiteral of(boolean value) {
        return value ? TRUE : FALSE;
    }

    public boolean value() {
        return value;
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        return value;
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitBooleanLiteral(this, parameter);
    }

    @Override
    public String toFilterString() {
        return value ? "(true)" : "(false)";
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
