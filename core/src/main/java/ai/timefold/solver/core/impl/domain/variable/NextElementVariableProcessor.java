package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.LocationInList;

public class NextElementVariableProcessor<Solution_>
        extends AbstractNextPrevElementVariableProcessor<Solution_> {

    protected final NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    protected final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public NextElementVariableProcessor(
            NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable,
            Object originalElement, LocationInList originalElementLocation) {
        System.out.println("Add " + originalElement + " " + originalElementLocation);
        var index = originalElementLocation.index();
        var next = index == listVariable.size() - 1 ? null : listVariable.get(index + 1);
        if (shadowVariableDescriptor.getValue(originalElement) != next) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, originalElement);
            shadowVariableDescriptor.setValue(originalElement, next);
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, originalElement);
        }
    }

    @Override
    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        System.out.println("Rem " + element);
        if (shadowVariableDescriptor.getValue(element) != null) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, null);
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object originalElement,
            LocationInList originalElementLocation) {
        var index = originalElementLocation.index();
        if (index == listVariable.size() - 1) {
            removeElement(scoreDirector, originalElement);
        } else {
            addElement(scoreDirector, listVariable, originalElement, originalElementLocation);
        }
    }
}
