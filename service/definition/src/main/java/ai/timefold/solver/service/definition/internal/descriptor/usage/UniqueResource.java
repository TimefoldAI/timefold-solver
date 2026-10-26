package ai.timefold.solver.service.definition.internal.descriptor.usage;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a resource type and a JSONPath expression to the ID field of the respective resource in the input payload.
 */
@NullMarked
public record UniqueResource(String resourceType, String resourceIdJsonPath) {

    public UniqueResource {
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        Objects.requireNonNull(resourceIdJsonPath, "resourceIdJsonPath must not be null");
    }
}
