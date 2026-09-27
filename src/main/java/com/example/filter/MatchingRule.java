package com.example.filter;

/**
 * Defines how the values of a particular property are compared.
 *
 * Resources carry every value as a {@code String} but comparing properties requires value interpretation.
 * A matching rule states the intent once per property so that evaluators can apply consistent semantics.
 *
 * Built-in rules are provided by {@link MatchingRules} and third parties may add their own by implementing this interface.
 *
 * Implementations are expected to be immutable and thread-safe.
 */
public interface MatchingRule {

    /** Short name for diagnostics */
    String name();

    /**
     * Compares a value stored on a resource against an operand taken from a filter.
     *
     * A null result is an important case that means the property value cannot be interpreted under this rule.
     *
     * @param propertyValue the value stored on the resource
     * @param operand       the operand supplied by the filter
     * @return a negative number, zero or a positive number as {@code propertyValue} is
     *         less than, equal to or greater than {@code operand}; or {@code null} if
     *         {@code propertyValue} cannot be interpreted under this rule, in which case
     *         the comparison does not match
     */
    Integer compare(String propertyValue, String operand);

    /**
     * Validates an operand when a filter is built or parsed.
     *
     * Rejecting a bad operand rather than silently failing to match at
     * evaluation time means {@code (age>not-a-number)} fails once, immediately, and
     * identically no matter which back end would have run it. The default accepts
     * everything.
     *
     * @throws InvalidOperandException if the operand is not valid under this rule
     */
    default void validateOperand(String operand) {
        // Accepted by default.
    }
}
