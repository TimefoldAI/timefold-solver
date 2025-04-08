package ai.timefold.solver.core.impl.phase.scope;

import java.util.Random;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractStepScope<Solution_> {

    protected final int stepIndex;

    protected InnerScore<?> score = null;
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

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> InnerScore<Score_> getScore() {
        return (InnerScore<Score_>) score;
    }

    @SuppressWarnings("rawtypes")
    public void setInitializedScore(Score<?> score) {
        setScore(InnerScore.fullyAssigned((Score) score));
    }

    public void setScore(InnerScore<?> score) {
        this.score = score;
    }

    public boolean getBestScoreImproved() {
        return bestScoreImproved;
    }

    public void setBestScoreImproved(Boolean bestScoreImproved) {
        this.bestScoreImproved = bestScoreImproved;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return getPhaseScope().getScoreDirector();
    }

    public <Score_ extends Score<Score_>> MoveDirector<Solution_, Score_> getMoveDirector() {
        return this.<Score_> getScoreDirector().getMoveDirector();
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
