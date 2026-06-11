package ai.timefold.solver.service.definition.api.description;

import java.util.Arrays;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Groups related constraints into a single category for model description.
 *
 * @param id Stable unique identifier of the constraint group.
 * @param name Human-readable name of the constraint group.
 * @param description Explanation of what constraints in this group represent.
 * @param icon Icon name for this constraint group. Any icon name from <a href="https://tabler.io/icons">Tabler Icons</a>
 *        is valid.
 * @param tags Optional tags that can be used to classify or filter the constraint group.
 */
@NullMarked
public record ConstraintGroupInfo(String id, @Nullable String name, @Nullable String description, @Nullable String icon,
        @Nullable String[] tags) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConstraintGroupInfo that))
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
        return "ConstraintGroupInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", icon='" + icon + '\'' +
                ", tags=" + Arrays.toString(tags) +
                '}';
    }
}