package ai.timefold.solver.core.api.score.stream;

import java.util.Collection;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Provides information about the known constraints.
 * Works in combination with {@link ConstraintProvider}.
 */
@NullMarked
public interface ConstraintMetaModel {

    /**
     * Returns the constraint for the given reference.
     *
     * @return null if such constraint does not exist
     */
    @Nullable
    Constraint getConstraint(ConstraintRef constraintRef);

    /**
     * Returns the constraint with the given id.
     * Convenience shorthand for {@link #getConstraint(ConstraintRef)}.
     *
     * @return null if such constraint does not exist
     */
    default @Nullable Constraint getConstraint(String id) {
        return getConstraint(ConstraintRef.of(id));
    }

    /**
     * Returns all constraints defined in the {@link ConstraintProvider}.
     *
     * @return iteration order is undefined
     */
    Collection<Constraint> getConstraints();

}
