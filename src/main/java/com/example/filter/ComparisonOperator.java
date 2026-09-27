package com.example.filter;

/**
 * The relational operators supported by {@link PropertyComparison}.
 *
 * Operators are independent of how values are actually compared (see
 * {@link MatchingRule}).
 */
public enum ComparisonOperator {

    EQUAL_TO("="),
    NOT_EQUAL_TO("!="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL_TO("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL_TO(">=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /**
     * Applies this operator to the result of comparing the property value against the
     * operand value.
     *
     * @param comparisonResult negative, zero or positive as per {@link Comparable}
     */
    public boolean test(int comparisonResult) {
        return switch (this) {
            case EQUAL_TO -> comparisonResult == 0;
            case NOT_EQUAL_TO -> comparisonResult != 0;
            case LESS_THAN -> comparisonResult < 0;
            case LESS_THAN_OR_EQUAL_TO -> comparisonResult <= 0;
            case GREATER_THAN -> comparisonResult > 0;
            case GREATER_THAN_OR_EQUAL_TO -> comparisonResult >= 0;
        };
    }

    public static ComparisonOperator fromSymbol(String symbol) {
        for (ComparisonOperator operator : values()) {
            if (operator.symbol.equals(symbol)) {
                return operator;
            }
        }
        throw new IllegalArgumentException("Unknown comparison operator: " + symbol);
    }
}
