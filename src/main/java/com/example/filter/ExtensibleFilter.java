package com.example.filter;

import java.util.List;
import java.util.Map;

/**
 * Interface for filter types outside of the core filter vocabulary.
 *
 * Core filter types participate in the visitor contract defined by {@link FilterVisitor}.
 * Extension filter types cannot participate because they are open-ended and may not exist
 * when a visitor is compiled.
 *
 * Extension filters expose a small structural protocol that generic consumers can rely on:

 * {@link #extensionId()} identifies the filter so a consumer can can apply extension-specific handling.
 * {@link #operands()} exposes nested filters so consumers can recurse through an extension they do not understand.
 * {@link #parameters()} exposes the extension's own configuration as data.
 */
public interface ExtensibleFilter extends Filter {

    /**
     * A stable identifier for this filter type, unique across all integrations.
     *
     * Use a namespaced value like a URN to avoid collisions.
     */
    String extensionId();

    /**
     * Nested filters, if this extension composes others.
     *
     * Exposing operands is what allows generic tooling to traverse the whole tree.
     */
    default List<Filter> operands() {
        return List.of();
    }

    /** This extension's configuration as data */
    default Map<String, String> parameters() {
        return Map.of();
    }

    @Override
    default <R, P> R accept(FilterVisitor<R, P> visitor, P parameter) {
        return visitor.visitExtension(this, parameter);
    }
}
