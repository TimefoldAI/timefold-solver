package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ExternalizedPreviousElementVariableProcessor<Solution_>
        extends AbstractExternalizedNextPrevElementVariableProcessor<Solution_> {

    public ExternalizedPreviousElementVariableProcessor(
            PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        super(shadowVariableDescriptor);
    }

    @Override
    public void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            Integer index) {
        var previous = index == 0 ? null : listVariable.get(index - 1);
        setValue(scoreDirector, element, previous);
    }

    public Object getElement(Object element) {
        return shadowVariableDescriptor.getValue(element);
    }

}
