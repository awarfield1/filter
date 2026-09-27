package com.example.filter;

/**
 * Thrown when a filter operand is not valid under the {@link MatchingRule}
 */
public class InvalidOperandException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final String operand;
    private final String rule;

    public InvalidOperandException(String operand, String rule, String detail) {
        super("Invalid operand \"" + operand + "\" for matching rule " + rule + ": " + detail);
        this.operand = operand;
        this.rule = rule;
    }

    public String getOperand() {
        return operand;
    }

    public String getRule() {
        return rule;
    }
}
