package ai.timefold.solver.service.definition.internal.descriptor;

/**
 * Describes a single constraints and its parameters.
 *
 * @param name the human-readable name serving as the unique identifier
 * @param description the human-readable description
 * @param defaultWeight default weight of the constraint
 */
public record ConstraintDescriptor(String id, String name, String description, String defaultWeight) {
}
