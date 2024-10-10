package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.score.director.ScoreDirector;

public interface InnerMutableSolutionState<Solution_> extends MutableSolutionState<Solution_> {

    ScoreDirector<Solution_> getVariableChangeRecordingScoreDirector();

}
