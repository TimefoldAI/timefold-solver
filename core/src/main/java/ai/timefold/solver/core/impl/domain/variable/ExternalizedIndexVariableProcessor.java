package ai.timefold.solver.core.impl.domain.variable;

import java.util.Objects;

import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ExternalizedIndexVariableProcessor<Solution_> {

    private final IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    public ExternalizedIndexVariableProcessor(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
    }

    public boolean addElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        return updateIndex(scoreDirector, element, index);
    }

    public boolean removeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        return updateIndex(scoreDirector, element, null);
    }

    public boolean unassignElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        return removeElement(scoreDirector, element);
    }

    public boolean changeElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        return updateIndex(scoreDirector, element, index);
    }

    private boolean updateIndex(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Integer index) {
        var oldIndex = getIndex(element);
        if (!Objects.equals(oldIndex, index)) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, index);
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
            return true;
        }
        return false;
    }

    public Integer getIndex(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }

}
