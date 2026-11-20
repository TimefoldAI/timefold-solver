package ai.timefold.solver.service.definition.internal.descriptor.usage;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a resource type and a JSONPath expression to the ID field of the respective resource in the input payload.
 */
@NullMarked
public record UniqueResource(String resourceType, String resourceIdJsonPath) {

    public UniqueResource {
        if (resourceType == null || resourceType.trim().isEmpty()) {
            throw new IllegalArgumentException("resourceType must not be empty");
        }
        if (resourceIdJsonPath == null || resourceIdJsonPath.trim().isEmpty()) {
            throw new IllegalArgumentException("resourceIdJsonPath must not be empty");
        }
    }
}
