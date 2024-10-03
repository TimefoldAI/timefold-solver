package ai.timefold.solver.core.api.score.stream;

import java.util.Collection;
import java.util.Set;

import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provides information about the known constraints.
 * Works in combination with {@link ConstraintProvider}.
 */
public interface ConstraintMetaModel {

    /**
     * Returns the constraint for the given reference.
     *
     * @return null if such constraint does not exist
     */
    @Nullable
    Constraint getConstraint(@NonNull ConstraintRef constraintRef);

    /**
     * Returns all constraints defined in the {@link ConstraintProvider}.
     *
     * @return iteration order is undefined
     */
    @NonNull
    Collection<Constraint> getConstraints();

    /**
     * Returns all constraints from {@link #getConstraints()} that belong to the given group.
     *
     * @return iteration order is undefined
     */
    @NonNull
    Collection<Constraint> getConstraintsPerGroup(@NonNull String constraintGroup);

    /**
     * Returns constraint groups with at least one constraint in it.
     *
     * @return iteration order is undefined
     */
    @NonNull
    Set<String> getConstraintGroups();

}
