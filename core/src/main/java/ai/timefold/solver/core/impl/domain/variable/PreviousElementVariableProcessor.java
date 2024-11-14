package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.LocationInList;

final class PreviousElementVariableProcessor<Solution_>
        extends AbstractNextPrevElementVariableProcessor<Solution_> {

    private final PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public PreviousElementVariableProcessor(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
    }

    @Override
    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable,
            Object originalElement, LocationInList originalElementLocation) {
        var index = originalElementLocation.index();
        var previous = index == 0 ? null : listVariable.get(originalElementLocation.index() - 1);
        setValue(scoreDirector, originalElement, previous);
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
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object originalElement,
            LocationInList originalElementLocation) {
        var index = originalElementLocation.index();
        if (index == 0) {
            setValue(scoreDirector, originalElement, null);
        } else {
            setValue(scoreDirector, originalElement, listVariable.get(index - 1));
        }
    }
}
