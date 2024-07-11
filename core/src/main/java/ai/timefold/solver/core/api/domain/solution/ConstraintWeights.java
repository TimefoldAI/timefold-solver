package ai.timefold.solver.core.api.domain.solution;

import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;

/**
 * Used to override constraint weights defined in Constraint Streams,
 * e.g., in {@link UniConstraintStream#penalize(Score)}.
 * To use,
 * place a member (typically a field) of type {@link ConstraintWeights} in your {@link PlanningSolution}-annotated class.
 * <p>
 * Users should use {@link #setConstraintWeight(String, Score)} for every constraint weight they want to override.
 * Alternatively, a JSON serializers and deserializer may be defined to interact with a solution file.
 * <p>
 * This feature does not support constraints in user-defined packages, which is a deprecated feature in itself.
 * The constraint is assumed to be in the same package as the top-most class implementing this interface.
 * It is therefore required that the constraints be built using {@link UniConstraintBuilder#asConstraint(String)},
 * leaving the constraint package to its default value.
 *
 * @param <Score_>
 */
public interface ConstraintWeights<Score_ extends Score<Score_>> {

    private static ConstraintRef determineConstraintRef(String constraintName, ConstraintWeights<?> weights) {
        var packageName = weights.getClass().getPackageName();
        return ConstraintRef.of(packageName, constraintName);
    }

    default void setConstraintWeight(String constraintName, Score_ constraintWeight) {
        setConstraintWeight(determineConstraintRef(constraintName, this), constraintWeight);
    }

    void setConstraintWeight(ConstraintRef constraintRef, Score_ constraintWeight);

    /**
     * Return a constraint weight for a particular constraint.
     *
     * @param constraintName never null
     * @return zero if the constraint name is not known
     */
    default Score_ getConstraintWeight(String constraintName) {
        return getConstraintWeight(determineConstraintRef(constraintName, this));
    }

    /**
     * Return a constraint weight for a particular constraint.
     *
     * @param constraintRef never null
     * @return zero if the constraint name is not known
     */
    Score_ getConstraintWeight(ConstraintRef constraintRef);

    /**
     * Returns all known constraints.
     * 
     * @return All {@link ConstraintRef}s for which {@link #isKnown(ConstraintRef)} is true.
     */
    Set<ConstraintRef> getKnownConstraints();

    /**
     * Returns whether the constraint has a constraint weight assigned,
     * either directly via {@link #setConstraintWeight(ConstraintRef, Score)}
     * or via some deserialization mechanism
     *
     * @param constraintRef never null
     * @return true if the constraint weight is set, even if set to zero
     */
    boolean isKnown(ConstraintRef constraintRef);

    /**
     * Returns whether the constraint has a constraint weight assigned,
     * either directly via {@link #setConstraintWeight(ConstraintRef, Score)}
     * or via some deserialization mechanism
     *
     * @param constraintName never null
     * @return true if the constraint weight is set, even if set to zero
     */
    default boolean isKnown(String constraintName) {
        return isKnown(determineConstraintRef(constraintName, this));
    }

}
