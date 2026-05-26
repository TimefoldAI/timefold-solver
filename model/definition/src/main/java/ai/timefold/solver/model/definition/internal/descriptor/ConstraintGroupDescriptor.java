package ai.timefold.solver.model.definition.internal.descriptor;

import java.util.Arrays;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConstraintGroupDescriptor that))
            return false;

        return id.equals(that.id) && Objects.equals(name, that.name) && Objects.equals(icon, that.icon)
                && Arrays.equals(tags, that.tags) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(description);
        result = 31 * result + Objects.hashCode(icon);
        result = 31 * result + Arrays.hashCode(tags);
        return result;
    }

    @Override
    public String toString() {
        return "ConstraintGroupDescriptor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", constraintDescriptors=" + Arrays.toString(constraintDescriptors) +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}
