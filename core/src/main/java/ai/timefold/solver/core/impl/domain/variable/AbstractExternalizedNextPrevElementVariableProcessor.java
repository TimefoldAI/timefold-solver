package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

abstract sealed class AbstractExternalizedNextPrevElementVariableProcessor<Solution_>
        permits ExternalizedNextElementVariableProcessor, ExternalizedPreviousElementVariableProcessor {

    protected final ShadowVariableDescriptor<Solution_> shadowVariableDescriptor;

    protected AbstractExternalizedNextPrevElementVariableProcessor(
            ShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        this.shadowVariableDescriptor = Objects.requireNonNull(shadowVariableDescriptor);
    }

    public abstract void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index);

    public abstract Object getElement(Object element);

    public void unsetElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        setValue(scoreDirector, element, null);
    }

    protected void setValue(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Object value) {
        if (getElement(element) != value) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, value);
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        }
    }

}
