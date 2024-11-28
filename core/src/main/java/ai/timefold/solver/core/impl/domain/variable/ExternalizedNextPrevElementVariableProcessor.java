package ai.timefold.solver.core.impl.domain.variable;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

final class ExternalizedNextPrevElementVariableProcessor<Solution_> {

    public static <Solution_> ExternalizedNextPrevElementVariableProcessor<Solution_>
            ofPrevious(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        return new ExternalizedNextPrevElementVariableProcessor<>(shadowVariableDescriptor, -1);
    }

    public static <Solution_> ExternalizedNextPrevElementVariableProcessor<Solution_>
            ofNext(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        return new ExternalizedNextPrevElementVariableProcessor<>(shadowVariableDescriptor, 1);
    }

    private final ShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    private final int modifier;

    private ExternalizedNextPrevElementVariableProcessor(ShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            int modifier) {
        this.shadowVariableDescriptor = Objects.requireNonNull(shadowVariableDescriptor);
        this.modifier = modifier;
    }

    public void setElement(InnerScoreDirector<Solution_, ?> scoreDirector, List<Object> listVariable, Object element,
            int index) {
        var target = index + modifier;
        if (target < 0 || target >= listVariable.size()) {
            setValue(scoreDirector, element, null);
        } else {
            setValue(scoreDirector, element, listVariable.get(target));
        }
    }

    private void setValue(InnerScoreDirector<Solution_, ?> scoreDirector, Object element, Object value) {
        if (getElement(element) != value) {
            scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, value);
            scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        }
    }

    public Object getElement(Object element) {
        return shadowVariableDescriptor.getValue(element);
    }

    public void unsetElement(InnerScoreDirector<Solution_, ?> scoreDirector, Object element) {
        setValue(scoreDirector, element, null);
    }

}
