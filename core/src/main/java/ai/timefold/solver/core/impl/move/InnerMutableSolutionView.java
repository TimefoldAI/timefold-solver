package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerMutableSolutionView<Solution_> extends MutableSolutionView<Solution_> {

    VariableDescriptorAwareScoreDirector<Solution_> getScoreDirector();

}
