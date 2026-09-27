package com.example.filter.visitor;

import com.example.filter.And;
import com.example.filter.BooleanLiteral;
import com.example.filter.ComparisonOperator;
import com.example.filter.ExtensibleFilter;
import com.example.filter.Filter;
import com.example.filter.FilterVisitor;
import com.example.filter.MatchingRules;
import com.example.filter.Not;
import com.example.filter.Or;
import com.example.filter.PropertyComparison;
import com.example.filter.PropertyMatchesRegex;
import com.example.filter.PropertyPresent;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * A canonical visitor that renders filters as human-readable descriptions.
 *
 * Core filter types are rendered as English expressions that reflect their semantics.
 *
 * Extension filters are rendered generically using their extension id, parameters, and operands
 * because this visitor cannot interpret extension-specific semantics.
 *
 * Extension descriptions use the form {@code extension <extensionId>}. Parameters
 * are appended as {@code with <name>="<value>"}, and nested filters are introduced
 * as {@code operands: (...)} without implying how the extension interprets them.
 *
 * This visitor is intended for auditing, logging, and debugging.
 *
 * Example:
 *
 *  Filter filter = and(
 *          equalTo("role", "admin"),
 *          greaterThan("age", "30"));
 *
 *  FilterDescriber.describe(filter);
 *
 * Output:
 *
 *  "role" is equal to "admin" and "age" is greater than "30"
 */
public final class FilterDescriber implements FilterVisitor<String, Void> {

    private static final FilterDescriber INSTANCE = new FilterDescriber();

    private FilterDescriber() {
    }

    /** Returns a human-readable description of the filter. */
    public static String describe(Filter filter) {
        return filter.accept(INSTANCE, null);
    }

    @Override
    public String visitBooleanLiteral(BooleanLiteral filter, Void unused) {
        return filter.value() ? "anything" : "nothing";
    }

    @Override
    public String visitAnd(And filter, Void unused) {
        return join(filter.operands(), " and ");
    }

    @Override
    public String visitOr(Or filter, Void unused) {
        return join(filter.operands(), " or ");
    }

    @Override
    public String visitNot(Not filter, Void unused) {
        return "not (" + filter.operand().accept(this, null) + ")";
    }

    @Override
    public String visitPropertyPresent(PropertyPresent filter, Void unused) {
        return quote(filter.property()) + " is present";
    }

    @Override
    public String visitPropertyComparison(PropertyComparison filter, Void unused) {
        String description = quote(filter.property())
                + " is " + phraseFor(filter.operator())
                + " " + quote(filter.value());
        // Only mention the rule when it is not the default to keep the common case clean
        if (filter.matchingRule() == MatchingRules.AUTO) {
            return description;
        }
        return description + " (" + filter.matchingRule().name().toLowerCase() + ")";
    }

    @Override
    public String visitPropertyMatchesRegex(PropertyMatchesRegex filter, Void unused) {
        return quote(filter.property()) + " matches /" + filter.regex() + "/";
    }

    /** Renders an extension filter as a generic description without interpreting its semantics. */
    @Override
    public String visitExtension(ExtensibleFilter filter, Void unused) {
        StringBuilder description = new StringBuilder("extension ").append(filter.extensionId());
        Map<String, String> parameters = filter.parameters();
        if (!parameters.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ", " with ", "");
            parameters.forEach((name, value) -> joiner.add(name + "=" + quote(value)));
            description.append(joiner);
        }
        List<Filter> operands = filter.operands();
        if (!operands.isEmpty()) {
            description.append("; operands: (")
                    .append(join(operands, ", "))
                    .append(')');
        }
        return description.toString();
    }

    private String join(List<Filter> operands, String separator) {
        StringJoiner joiner = new StringJoiner(separator);
        for (Filter operand : operands) {
            String described = operand.accept(this, null);
            // Wrap nested booleans in parentheses to avoid ambiguity in the English sentence
            joiner.add(operand instanceof And || operand instanceof Or ? "(" + described + ")" : described);
        }
        return joiner.toString();
    }

    private String phraseFor(ComparisonOperator operator) {
        return switch (operator) {
            case EQUAL_TO -> "equal to";
            case NOT_EQUAL_TO -> "present and not equal to";
            case LESS_THAN -> "less than";
            case LESS_THAN_OR_EQUAL_TO -> "less than or equal to";
            case GREATER_THAN -> "greater than";
            case GREATER_THAN_OR_EQUAL_TO -> "greater than or equal to";
        };
    }

    private String quote(String value) {
        return '"' + value + '"';
    }
}
