package com.example.filter;

import java.util.Objects;

/**
 * Validation rules for property names.
 *
 * Property names cannot contain characters that have meaning in the filter grammar.
 */
final class PropertyNames {

    static final String RESERVED = "()=<>!~&|*\\";

    private PropertyNames() {
    }

    static void validate(String property) {
        Objects.requireNonNull(property, "property");
        if (property.isEmpty()) {
            throw new IllegalArgumentException("Property name must not be empty");
        }
        for (int i = 0; i < property.length(); i++) {
            char c = property.charAt(i);
            if (Character.isWhitespace(c) || RESERVED.indexOf(c) >= 0) {
                throw new IllegalArgumentException(
                        "Illegal character '" + c + "' in property name \"" + property + "\"");
            }
        }
    }

    static boolean isNameChar(char c) {
        return !Character.isWhitespace(c) && RESERVED.indexOf(c) < 0;
    }
}
