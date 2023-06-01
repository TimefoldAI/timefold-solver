package ai.timefold.solver.enterprise.multithreaded;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.Move;

final class ApplyStepOperation<Solution_, Score_ extends Score<Score_>> extends MoveThreadOperation<Solution_> {

    private final int stepIndex;
    private final Move<Solution_> step;
    private final Score_ score;

    public ApplyStepOperation(int stepIndex, Move<Solution_> step, Score_ score) {
        this.stepIndex = stepIndex;
        this.step = step;
        this.score = score;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public Move<Solution_> getStep() {
        return step;
    }

    public Score_ getScore() {
        return score;
    }

}
