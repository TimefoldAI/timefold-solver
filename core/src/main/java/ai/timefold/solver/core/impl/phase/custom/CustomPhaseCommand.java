package ai.timefold.solver.core.impl.phase.custom;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.ProblemFactChange;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * Runs a custom algorithm as a {@link Phase} of the {@link Solver} that changes the planning variables.
 * Do not abuse to change the problems facts,
 * instead use {@link Solver#addProblemFactChange(ProblemFactChange)} for that.
 * <p>
 * To add custom properties, configure custom properties and add public setters for them.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@FunctionalInterface
public interface CustomPhaseCommand<Solution_> {

    /**
     * Changes {@link PlanningSolution working solution} of {@link ScoreDirector#getWorkingSolution()}.
     * When the {@link PlanningSolution working solution} is modified, the {@link ScoreDirector} must be correctly notified
     * (through {@link ScoreDirector#beforeVariableChanged(Object, String)} and
     * {@link ScoreDirector#afterVariableChanged(Object, String)}),
     * otherwise calculated {@link Score}s will be corrupted.
     * <p>
     * Don't forget to call {@link ScoreDirector#triggerVariableListeners()} after each set of changes
     * (especially before every {@link InnerScoreDirector#calculateScore()} call)
     * to ensure all shadow variables are updated.
     *
     * @param scoreDirector never null, the {@link ScoreDirector} that needs to get notified of the changes.
     */
    void changeWorkingSolution(ScoreDirector<Solution_> scoreDirector);

    /**
     * By default,
     * when the solution returned by the custom phase is worse than the {@link AbstractPhaseScope#getStartingScore() starting
     * solution} from the phase,
     * it is expected to be ignored as it needs to improve the current best solution.
     * <p>
     * However, in some cases,
     * the current best solution needs to be updated with a new one to avoid losing the result
     * and ending up in an inconsistent state for the next phase.
     * <p>
     * For example, let's consider a custom construction heuristic phase for a model
     * using a planning list variable that accepts unassigned values.
     * The initial score might be better than the result of the custom phase, as some constraints may be violated.
     * That doesn't mean the solution should not be accepted, as the phase is building an initial solution.
     * 
     * @return false, update the best solution only if it is improved;
     *         otherwise, update it whichever the score is.
     */
    default boolean requireUpdateBestSolution() {
        return false;
    }

}
