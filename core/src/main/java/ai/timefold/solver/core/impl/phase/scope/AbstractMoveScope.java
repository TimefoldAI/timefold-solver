package ai.timefold.solver.core.impl.phase.scope;

import java.util.Random;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public abstract class AbstractMoveScope<Solution_> {

    protected final AbstractStepScope<Solution_> stepScope;
    protected final int moveIndex;
    protected final Move<Solution_> move;

    protected Score<?> score = null;

    protected AbstractMoveScope(AbstractStepScope<Solution_> stepScope, int moveIndex, Move<Solution_> move) {
        this.stepScope = stepScope;
        this.moveIndex = moveIndex;
        this.move = move;
    }

    public AbstractStepScope<Solution_> getStepScope() {
        return stepScope;
    }

    public int getMoveIndex() {
        return moveIndex;
    }

    public Move<Solution_> getMove() {
        return move;
    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public int getStepIndex() {
        return getStepScope().getStepIndex();
    }

    public <Score_ extends Score<Score_>> InnerScoreDirector<Solution_, Score_> getScoreDirector() {
        return getStepScope().getScoreDirector();
    }

    public Solution_ getWorkingSolution() {
        return getStepScope().getWorkingSolution();
    }

    public Random getWorkingRandom() {
        return getStepScope().getWorkingRandom();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getStepScope().getStepIndex() + "/" + moveIndex + ")";
    }

}
