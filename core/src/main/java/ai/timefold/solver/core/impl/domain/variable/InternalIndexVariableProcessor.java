package ai.timefold.solver.core.impl.domain.variable;

import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

public final class InternalIndexVariableProcessor<Solution_>
        implements IndexVariableProcessor<Solution_> {

    private final IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public InternalIndexVariableProcessor(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
    }

    @Override
    public void addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        updateIndex(scoreDirector, element, index);
    }

    @Override
    public void removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        updateIndex(scoreDirector, element, null);
    }

    @Override
    public void unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        updateIndex(scoreDirector, element, null);
    }

    @Override
    public void changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        updateIndex(scoreDirector, element, index);
    }

    private void updateIndex(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        var oldIndex = shadowVariableDescriptor.getValue(element);
        if (!Objects.equals(oldIndex, index)) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, index);
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public Integer getIndex(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }

}
