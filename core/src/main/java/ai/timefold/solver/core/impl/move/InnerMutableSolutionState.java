package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public interface InnerMutableSolutionState<Solution_> extends MutableSolutionState<Solution_> {

    VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector();

}
