package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class PreviousElementVariableProcessor<Solution_>
        extends AbstractNextPrevElementVariableProcessor<Solution_> {

    public PreviousElementVariableProcessor(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        super(shadowVariableDescriptor);
    }

    @Override
    public void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index) {
        var previous = index == 0 ? null : listVariable.get(index - 1);
        setValue(scoreDirector, element, previous);
    }

}
