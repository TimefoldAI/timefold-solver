package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * This represents a single constraint in the {@link ConstraintStream} API
 * that impacts the {@link Score}.
 * It is defined in {@link ConstraintProvider#defineConstraints(ConstraintFactory)}
 * by calling {@link ConstraintFactory#forEach(Class)}.
 */
public interface Constraint {

    String DEFAULT_CONSTRAINT_GROUP = "default";

    ConstraintRef getConstraintRef();

    /**
     * Returns a human-friendly description of the constraint.
     * The format of the description is left unspecified and will not be parsed in any way.
     *
     * @return may be left empty
     */
    default @NonNull String getDescription() {
        return "";
    }

    default @NonNull String getConstraintGroup() {
        return DEFAULT_CONSTRAINT_GROUP;
    }

    /**
     * Returns the weight of the constraint as defined in the {@link ConstraintProvider},
     * without any overrides.
     *
     * @return null if the constraint does not have a weight defined
     */
    default <Score_ extends Score<Score_>> @Nullable Score_ getConstraintWeight() {
        return null;
    }

}
