package ai.timefold.solver.service.definition.api.description;

import ai.timefold.solver.core.api.score.stream.ConstraintMetadata;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Describes a score constraint in a form suitable for model descriptors and other consumer-facing metadata.
 * <p>
 * Instances of this record are attached to constraints through
 * {@link ai.timefold.solver.core.api.score.stream.ConstraintBuilder#asConstraint(ConstraintMetadata)} and are later used
 * to expose stable identifiers, user-facing names, and grouping information.
 *
 * @param id Stable unique identifier of the constraint.
 * @param name Human-readable name of the constraint.
 * @param description Explanation of the constraint and its goal.
 * @param constraintGroup {@link ConstraintGroupInfo} of a constraint groups this constraint belongs to.
 */
@NullMarked
public record ConstraintInfo(String id, @Nullable String name, @Nullable String description,
        @Nullable ConstraintGroupInfo constraintGroup) implements ConstraintMetadata {
}
