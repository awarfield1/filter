package com.example.filter;

/**
 * Typesafe double-dispatch over the core filter vocabulary.
 *
 * This is the extension point for third-party code that needs to act on the
 * structure of a filter rather than just evaluate it.
 *
 * The {@link com.example.filter.visitor} package ships two worked examples of visitors:
 *
 * {@link com.example.filter.visitor.FilterDescriber}
 * {@link com.example.filter.visitor.PropertyCollector}
 *
 * The visitor interface exposes dedicated methods for core filter types.
 *
 * Adding a core type is a deliberate source-level breaking change for visitor implementations.
 * This ensures every visitor must explicitly decide how to handle the new filter type,
 * making it impossible to accidentally ignore a newly introduced core filter.
 * This gives consumers exhaustive compile-time handling of the built-in filter model.
 *
 * Parameter {@code P} carries caller-supplied context during a traversal. This allows visitors to
 * receive state explicitly rather than storing it in fields.
 * Stateless visitors can therefore be reused safely across multiple traversals.
 *
 * Visitors that genuinely need no context use {@code Void} and pass {@code null}.
 *
 * Types the visitor cannot know about are routed to {@link #visitExtension(ExtensibleFilter, Object)}.
 * Unlike core filter types, extensions do not have dedicated visitor
 * methods because they are not known in advance. Consumers may inspect
 * their identifier, operands and parameters, reject them, or provide
 * extension-specific handling based on the extension's documented contract.
 *
 * @param <R> the result type produced by this visitor
 * @param <P> the context passed through the traversal; use {@link Void} if none is needed
 */
public interface FilterVisitor<R, P> {

    R visitBooleanLiteral(BooleanLiteral filter, P parameter);

    R visitAnd(And filter, P parameter);

    R visitOr(Or filter, P parameter);

    R visitNot(Not filter, P parameter);

    R visitPropertyPresent(PropertyPresent filter, P parameter);

    R visitPropertyComparison(PropertyComparison filter, P parameter);

    R visitPropertyMatchesRegex(PropertyMatchesRegex filter, P parameter);

    /**
     * Invoked for filter implementations outside the core filter vocabulary.
     *
     * The default implementation rejects the extension because a generic visitor
     * cannot infer the semantics of an unknown filter type.
     *
     * Override this to support custom filter types only when the visitor can
     * understand the semantics of the extension, either through its documented
     * extension contract or semantics common to all the extensions the visitor
     * is expected to handle.
     *
     * @throws UnsupportedFilterException always, unless overridden
     */
    default R visitExtension(ExtensibleFilter filter, P parameter) {
        throw new UnsupportedFilterException(filter);
    }
}
