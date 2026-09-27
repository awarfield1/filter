package com.example.filter;

import java.util.Map;
import java.util.Objects;

/**
 * Matches resources that have the named property, regardless of its value.
 *
 * String form: {@code (role=*)}
 *
 * @param property the (case-sensitive) property name
 */
public record PropertyPresent(String property) implements Filter {

    public PropertyPresent {
        PropertyNames.validate(property);
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource");
        return resource.get(property) != null;
    }

    @Override
    public <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitPropertyPresent(this, parameter);
    }

    @Override
    public String toFilterString() {
        return "(" + property + "=*)";
    }

    @Override
    public String toString() {
        return toFilterString();
    }
}
