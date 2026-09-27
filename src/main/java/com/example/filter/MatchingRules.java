package com.example.filter;

import java.math.BigDecimal;

/**
 * The built-in matching rules.
 *
 * AUTO is the default and preserves the behavior expected of an untyped property bag.
 *
 * The remaining rules state an intent explicitly.
 */
public enum MatchingRules implements MatchingRule {

    /**
     * The default rule for untyped properties.
     *
     * Compares numerically when both values look like numbers.
     * Case-insensitively comparison otherwise.
     *
     * Its semantics depend on the data:
     *
     * "10" sorts above "9" numerically but below it lexically.
     *
     * Declare {@link #NUMERIC} or {@link #CASE_IGNORE} whenever the intent is known.
     */
    AUTO {
        @Override
        public Integer compare(String propertyValue, String operand) {
            BigDecimal left = toNumber(propertyValue);
            if (left != null) {
                BigDecimal right = toNumber(operand);
                if (right != null) {
                    return left.compareTo(right);
                }
            }
            return propertyValue.compareToIgnoreCase(operand);
        }
    },

    /** Compares as text, ignoring case */
    CASE_IGNORE {
        @Override
        public Integer compare(String propertyValue, String operand) {
            return propertyValue.compareToIgnoreCase(operand);
        }
    },

    /** Compares as text, respecting case */
    CASE_EXACT {
        @Override
        public Integer compare(String propertyValue, String operand) {
            return propertyValue.compareTo(operand);
        }
    },

    /** Compares as decimal numbers. Unparseable operands fails immediately. */
    NUMERIC {
        @Override
        public Integer compare(String propertyValue, String operand) {
            BigDecimal left = toNumber(propertyValue);
            return left == null ? null : left.compareTo(new BigDecimal(operand.trim()));
        }

        @Override
        public void validateOperand(String operand) {
            if (toNumber(operand) == null) {
                throw new InvalidOperandException(operand, name(), "expected a decimal number");
            }
        }
    };

    private static BigDecimal toNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }
}
