package com.example.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses the canonical filter string representation.
 *
 * Examples:
 *
 *     (role=administrator)
 *     (&(role=administrator)(age>30))
 *     (!(status=disabled))
 *     (surname~=^bl)
 *
 * The syntax uses LDAP-style prefix notation. Every operation is explicitly parenthesized.
 *
 * Filter strings produced by {@link Filter#toFilterString()} can be
 * parsed back into equivalent core filter trees.
 *
 * Values may contain escaped special characters. Whitespace between filter elements is ignored.
 */
public final class FilterParser {

    private final String input;
    private final FilterSchema schema;
    private int position;

    private FilterParser(String input, FilterSchema schema) {
        this.input = input;
        this.schema = schema;
    }

    /**
     * Parses a filter string, applying {@link FilterSchema#DEFAULT}.
     *
     * @throws FilterParseException      if the input is not a well-formed filter
     */
    public static Filter parse(String filterString) {
        return parse(filterString, FilterSchema.DEFAULT);
    }

    /**
     * Parses a filter string, resolving each comparison's {@link MatchingRule} from the
     * given schema.
     *
     * @throws FilterParseException      if the input is not a well-formed filter
     * @throws InvalidOperandException   if an operand is invalid under the schema
     */
    public static Filter parse(String filterString, FilterSchema schema) {
        Objects.requireNonNull(filterString, "filterString");
        Objects.requireNonNull(schema, "schema");
        FilterParser parser = new FilterParser(filterString, schema);
        Filter filter = parser.parseFilter();
        parser.skipWhitespace();
        if (!parser.atEnd()) {
            throw parser.error("Unexpected trailing input");
        }
        return filter;
    }

    private Filter parseFilter() {
        skipWhitespace();
        expect('(');
        Filter filter = parseItem();
        skipWhitespace();
        expect(')');
        return filter;
    }

    private Filter parseItem() {
        skipWhitespace();
        return switch (peek()) {
            case '&' -> {
                position++;
                yield Filters.and(parseOperands());
            }
            case '|' -> {
                position++;
                yield Filters.or(parseOperands());
            }
            case '!' -> {
                position++;
                yield Filters.not(parseFilter());
            }
            default -> parseLiteralOrPredicate();
        };
    }

    private List<Filter> parseOperands() {
        List<Filter> operands = new ArrayList<>();
        skipWhitespace();
        while (!atEnd() && peek() == '(') {
            operands.add(parseFilter());
            skipWhitespace();
        }
        if (operands.isEmpty()) {
            throw error("Expected at least one operand");
        }
        return operands;
    }

    private Filter parseLiteralOrPredicate() {
        String name = parseName();
        skipWhitespace();
        if (!atEnd() && peek() == ')') {
            return switch (name) {
                case "true" -> BooleanLiteral.TRUE;
                case "false" -> BooleanLiteral.FALSE;
                default -> throw error("Expected an operator after property \"" + name + "\"");
            };
        }
        String symbol = parseOperatorSymbol();
        if ("=".equals(symbol) && !atEnd() && peek() == '*' && position + 1 < input.length()
                && input.charAt(position + 1) == ')') {
            position++;
            return new PropertyPresent(name);
        }
        String value = parseValue();
        if ("~=".equals(symbol)) {
            try {
                return new PropertyMatchesRegex(name, value);
            } catch (RuntimeException invalidRegex) {
                throw error("Invalid regular expression: " + invalidRegex.getMessage());
            }
        }
        return new PropertyComparison(name, ComparisonOperator.fromSymbol(symbol), value, schema.ruleFor(name));
    }

    private String parseName() {
        int start = position;
        while (!atEnd() && PropertyNames.isNameChar(peek())) {
            position++;
        }
        if (position == start) {
            throw error("Expected a property name");
        }
        return input.substring(start, position);
    }

    private String parseOperatorSymbol() {
        if (atEnd()) {
            throw error("Expected a comparison operator");
        }
        char first = peek();
        char second = position + 1 < input.length() ? input.charAt(position + 1) : '\0';
        switch (first) {
            case '!', '~' -> {
                if (second != '=') {
                    throw error("Expected '=' after '" + first + "'");
                }
                position += 2;
                return first + "=";
            }
            case '<', '>' -> {
                position++;
                if (second == '=') {
                    position++;
                    return first + "=";
                }
                return String.valueOf(first);
            }
            case '=' -> {
                position++;
                return "=";
            }
            default -> throw error("Expected a comparison operator but found '" + first + "'");
        }
    }

    private String parseValue() {
        StringBuilder value = new StringBuilder();
        while (!atEnd()) {
            char c = peek();
            if (c == ')') {
                return value.toString();
            }
            position++;
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (atEnd()) {
                throw error("Dangling escape character");
            }
            char escaped = input.charAt(position++);
            // Only syntax characters are unescaped
            if (escaped == '(' || escaped == ')' || escaped == '*' || escaped == '\\') {
                value.append(escaped);
            } else {
                value.append('\\').append(escaped);
            }
        }
        throw error("Unterminated value: missing ')'");
    }

    private void skipWhitespace() {
        while (!atEnd() && Character.isWhitespace(peek())) {
            position++;
        }
    }

    private void expect(char expected) {
        if (atEnd() || peek() != expected) {
            throw error("Expected '" + expected + "'");
        }
        position++;
    }

    private char peek() {
        if (atEnd()) {
            throw error("Unexpected end of input");
        }
        return input.charAt(position);
    }

    private boolean atEnd() {
        return position >= input.length();
    }

    private FilterParseException error(String message) {
        return new FilterParseException(message, input, position);
    }
}
