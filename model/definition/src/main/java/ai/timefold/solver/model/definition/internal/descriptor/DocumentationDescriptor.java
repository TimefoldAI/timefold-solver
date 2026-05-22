package ai.timefold.solver.model.definition.internal.descriptor;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the documentation support for a model.
 *
 * @param support the level of documentation support available
 * @param source an optional URL pointing to the documentation source
 */
public record DocumentationDescriptor(DocumentationSupport support, Optional<URL> source) {

    public static DocumentationDescriptor none() {
        return new DocumentationDescriptor(DocumentationSupport.NONE, Optional.empty());
    }

    public DocumentationDescriptor(DocumentationSupport support, URL source) {
        this(support, Optional.of(Objects.requireNonNull(source)));
    }
}
