package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class PreviousElementVariableProcessor<Solution_>
        extends AbstractNextPrevElementVariableProcessor<Solution_> {

    private final PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public PreviousElementVariableProcessor(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
    }

    @Override
    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index) {
        var previous = index == 0 ? null : listVariable.get(index - 1);
        setValue(scoreDirector, element, previous);
    }

    void setValue(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Object value) {
        if (shadowVariableDescriptor.getValue(element) != value) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, value);
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        setValue(scoreDirector, element, null);
    }

    @Override
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index) {
        if (index == 0) {
            setValue(scoreDirector, element, null);
        } else {
            setValue(scoreDirector, element, listVariable.get(index - 1));
        }
    }
}
