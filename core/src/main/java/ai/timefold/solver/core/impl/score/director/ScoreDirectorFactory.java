package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;

/**
 * Builds a {@link ScoreDirector}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface ScoreDirectorFactory<Solution_> {

    /**
     * Like {@link #buildScoreDirector(boolean, ConstraintMatchPolicy, boolean)},
     * with the final parameter set to true.
     *
     * @param lookUpEnabled true if a {@link ScoreDirector} implementation should track all working objects
     *        for {@link ScoreDirector#lookUpWorkingObject(Object)}
     * @param constraintMatchPolicy how should the {@link ScoreDirector} track {@link ConstraintMatch constraint matches}.
     * @return never null
     */
    default ScoreDirector<Solution_> buildScoreDirector(boolean lookUpEnabled, ConstraintMatchPolicy constraintMatchPolicy) {
        return buildScoreDirector(lookUpEnabled, constraintMatchPolicy, true);
    }

    ScoreDirector<Solution_> buildScoreDirector(boolean lookUpEnabled, ConstraintMatchPolicy constraintMatchPolicy,
            boolean expectShadowVariablesInCorrectState);

}
