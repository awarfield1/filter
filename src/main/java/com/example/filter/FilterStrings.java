package com.example.filter;

import java.util.List;

final class FilterStrings {

    private static final String MUST_ESCAPE = "()\\";

    private FilterStrings() {
    }

    /**
     * Escapes a value for inclusion in a filter string.
     *
     * Parentheses and backslashes are always escaped.
     * Asterisk only needs escaping when it is the entire value; anywhere else it is unambiguous.
     */
    static String escape(String value) {
        if ("*".equals(value)) {
            return "\\*";
        }
        StringBuilder out = null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (MUST_ESCAPE.indexOf(c) >= 0) {
                if (out == null) {
                    out = new StringBuilder(value.length() + 8).append(value, 0, i);
                }
                out.append('\\');
            }
            if (out != null) {
                out.append(c);
            }
        }
        return out == null ? value : out.toString();
    }

    static String composite(char operator, List<Filter> operands) {
        StringBuilder out = new StringBuilder(32).append('(').append(operator);
        for (Filter operand : operands) {
            out.append(operand.toFilterString());
        }
        return out.append(')').toString();
    }
}
