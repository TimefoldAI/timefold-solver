package ai.timefold.solver.model.definition.api.description;

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

}