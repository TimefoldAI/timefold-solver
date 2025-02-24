package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatch;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.director.MoveStreamSession;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;

/**
 * Builds a {@link ScoreDirector}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public interface ScoreDirectorFactory<Solution_> {

    /**
     * Like {@link #buildScoreDirector(MoveStreamSession, boolean, ConstraintMatchPolicy, boolean)},
     * with the final parameter set to true.
     *
     * @param moveStreamSession null if move streams not supported
     * @param lookUpEnabled true if a {@link ScoreDirector} implementation should track all working objects
     *        for {@link ScoreDirector#lookUpWorkingObject(Object)}
     * @param constraintMatchPolicy how should the {@link ScoreDirector} track {@link ConstraintMatch constraint matches}.
     * @return never null
     */
    default ScoreDirector<Solution_> buildScoreDirector(MoveStreamSession<Solution_> moveStreamSession, boolean lookUpEnabled,
            ConstraintMatchPolicy constraintMatchPolicy) {
        return buildScoreDirector(moveStreamSession, lookUpEnabled, constraintMatchPolicy, true);
    }

    ScoreDirector<Solution_> buildScoreDirector(MoveStreamSession<Solution_> moveStreamSession, boolean lookUpEnabled,
            ConstraintMatchPolicy constraintMatchPolicy, boolean expectShadowVariablesInCorrectState);

}
