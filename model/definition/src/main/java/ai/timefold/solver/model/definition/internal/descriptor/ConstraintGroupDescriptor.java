package ai.timefold.solver.model.definition.internal.descriptor;

/**
 * Describes a group constraints.
 *
 * @param id the group id
 * @param name the human-readable name
 * @param description the human-readable description
 * @param constraintDescriptors the descriptors of constraints that belong to this group
 * @param tags any string tags
 */
public record ConstraintGroupDescriptor(String id, String name, String description, String icon,
        ConstraintDescriptor[] constraintDescriptors, String[] tags) {
}
