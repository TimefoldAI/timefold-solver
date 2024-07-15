package ai.timefold.solver.core.api.domain.solution;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintBuilder;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintStream;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.domain.solution.DefaultConstraintWeightOverrides;

/**
 * Used to override constraint weights defined in Constraint Streams,
 * e.g., in {@link UniConstraintStream#penalize(Score)}.
 * To use,
 * place a member (typically a field) of type {@link ConstraintWeightOverrides}
 * in your {@link PlanningSolution}-annotated class.
 * <p>
 * Users should use {@link #of(Map)} to provide the actual constraint weights.
 * Alternatively, a JSON serializers and deserializer may be defined to interact with a solution file.
 * Once the constraint weights are set, they must remain constant throughout the solving process,
 * or a {@link ProblemChange} needs to be triggered.
 * <p>
 * Zero-weight will be excluded from processing,
 * and the solver will behave as if it did not exist in the {@link ConstraintProvider}.
 * <p>
 * There is no support for user-defined packages, which is a deprecated feature in itself.
 * The constraint is assumed to be in the same package as the top-most class implementing this interface.
 * It is therefore required that the constraints be built using {@link UniConstraintBuilder#asConstraint(String)},
 * leaving the constraint package to its default value.
 *
 * @param <Score_>
 */
public interface ConstraintWeightOverrides<Score_ extends Score<Score_>> {

    static <Score_ extends Score<Score_>> ConstraintWeightOverrides<Score_> none() {
        return of(Collections.<String, Score_> emptyMap());
    }

    static <Score_ extends Score<Score_>> ConstraintWeightOverrides<Score_> of(Map<String, Score_> constraintWeightMap) {
        return new DefaultConstraintWeightOverrides<>(constraintWeightMap);
    }

    /**
     * Return a constraint weight for a particular constraint.
     *
     * @param constraintName never null
     * @return null if the constraint name is not known
     */
    Score_ getConstraintWeight(String constraintName);

    /**
     * Returns all known constraints.
     * 
     * @return All constraint names for which {@link #getConstraintWeight(String)} returns a non-null value.
     */
    Set<String> getKnownConstraintNames();

}
