package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintWeight;
import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NonNull;

/**
 * Used by Constraint Streams' {@link Score} calculation.
 * An implementation must be stateless in order to facilitate building a single set of constraints
 * independent of potentially changing constraint weights.
 */
public interface ConstraintProvider {

    /**
     * This method is called once to create the constraints.
     * To create a {@link Constraint}, start with {@link ConstraintFactory#forEach(Class)}.
     *
     * @return an array of all {@link Constraint constraints} that could apply.
     *         The constraints with a zero {@link ConstraintWeight} for a particular problem
     *         will be automatically disabled when scoring that problem, to improve performance.
     */
    Constraint @NonNull [] defineConstraints(@NonNull ConstraintFactory constraintFactory);

}
