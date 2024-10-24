package ai.timefold.solver.core.impl.domain.variable.nextprev;

import java.util.List;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

public class PreviousElementVariableListener<Solution_> implements ListVariableListener<Solution_, Object, Object> {

    protected final PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    protected final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    public PreviousElementVariableListener(
            PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
            ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.shadowVariableDescriptor = shadowVariableDescriptor;
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> listVariable = sourceVariableDescriptor.getValue(entity);
        for (int i = 1; i < listVariable.size(); i++) {
            Object element = listVariable.get(i);
            Object previous = listVariable.get(i - 1);
            innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, previous);
            innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> listVariable = sourceVariableDescriptor.getValue(entity);
        for (int i = 1; i < listVariable.size(); i++) {
            Object element = listVariable.get(i);
            innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, null);
            innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        if (shadowVariableDescriptor.getValue(element) != null) {
            innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, null);
            innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity,
            int fromIndex, int toIndex) {
        // Do nothing
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity, int fromIndex,
            int toIndex) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> listVariable = sourceVariableDescriptor.getValue(entity);
        Object previous = fromIndex > 0 ? listVariable.get(fromIndex - 1) : null;
        for (int i = fromIndex; i <= toIndex && i < listVariable.size(); i++) {
            Object element = listVariable.get(i);
            if (previous != shadowVariableDescriptor.getValue(element)) {
                innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
                shadowVariableDescriptor.setValue(element, previous);
                innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
            }
            previous = element;
        }
    }
}
