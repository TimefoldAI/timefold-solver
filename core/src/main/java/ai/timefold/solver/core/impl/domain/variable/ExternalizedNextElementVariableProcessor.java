package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ExternalizedNextElementVariableProcessor<Solution_>
        extends AbstractExternalizedNextPrevElementVariableProcessor<Solution_> {

    public ExternalizedNextElementVariableProcessor(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        super(shadowVariableDescriptor);
    }

    @Override
    public void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index) {
        var next = index == listVariable.size() - 1 ? null : listVariable.get(index + 1);
        setValue(scoreDirector, element, next);
    }

    public Object getElement(Object element) {
        return shadowVariableDescriptor.getValue(element);
    }

}
