package ai.timefold.solver.core.impl.phase.scope;

import java.util.Random;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractStepScope<Solution_> {

    protected final int stepIndex;

    protected Score<?> score = null;
    protected boolean bestScoreImproved = false;
    // Stays null if there is no need to clone it
    protected Solution_ clonedSolution = null;

    public AbstractStepScope(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public abstract AbstractPhaseScope<Solution_> getPhaseScope();

    public int getStepIndex() {
        return stepIndex;
    }

    public Score<?> getScore() {
        return score;
    }

    public void setScore(Score<?> score) {
        this.score = score;
    }

    public boolean getBestScoreImproved() {
        return bestScoreImproved;
    }

    public void setBestScoreImproved(Boolean bestScoreImproved) {
        this.bestScoreImproved = bestScoreImproved;
    }

    public void incrementMoveEvaluationCount(Move<?> move) {
        if (getPhaseScope().isMetricCollectionEnabled()) {
            getPhaseScope().getSolverScope().addMoveEvaluationCount(1L);
            if (getPhaseScope().getSolverScope().isMetricEnabled(SolverMetric.MOVE_COUNT_PER_TYPE)) {
                getPhaseScope().getSolverScope().incrementMoveEvaluationCountPerType(move);
            }
        }
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************
    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return getPhaseScope().getScoreDirector();
    }

    public Solution_ getWorkingSolution() {
        return getPhaseScope().getWorkingSolution();
    }

    public Random getWorkingRandom() {
        return getPhaseScope().getWorkingRandom();
    }

    public Solution_ createOrGetClonedSolution() {
        if (clonedSolution == null) {
            clonedSolution = getScoreDirector().cloneWorkingSolution();
        }
        return clonedSolution;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + stepIndex + ")";
    }

}
