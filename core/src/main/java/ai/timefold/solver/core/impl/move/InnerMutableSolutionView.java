package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.move.MutableSolutionView;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

public interface InnerMutableSolutionView<Solution_> extends MutableSolutionView<Solution_> {

    VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector();

}
