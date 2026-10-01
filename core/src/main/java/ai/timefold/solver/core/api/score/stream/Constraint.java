package ai.timefold.solver.core.api.score.stream;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This represents a single constraint in the {@link ConstraintStream} API
 * that impacts the {@link Score}.
 * It is defined in {@link ConstraintProvider#defineConstraints(ConstraintFactory)}
 * by calling {@link ConstraintFactory#forEach(Class)}.
 */
@NullMarked
public interface Constraint {

    ConstraintRef getConstraintRef();

    /**
     * Returns the metadata for this constraint, as provided to
     * {@link ConstraintBuilder#asConstraint(ConstraintMetadata)}.
     * The constraint's identity ({@link ConstraintMetadata#id()}) is fixed at build time;
     * any later mutation of the returned object does not affect the constraint's identity.
     *
     * @return never null
     */
    ConstraintMetadata getConstraintMetadata();

    /**
     * Returns the weight of the constraint as defined in the {@link ConstraintProvider},
     * without any overrides.
     *
     * @return null if the constraint does not have a weight defined
     */
    <Score_ extends Score<Score_>> @Nullable Score_ getConstraintWeight();

}
