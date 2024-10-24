package ai.timefold.solver.core.impl.domain.variable.index;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

public class IndexVariableListener<Solution_> implements ListVariableListener<Solution_, Object, Object>, IndexVariableSupply {

    protected final IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor;
    protected final ListVariableDescriptor<Solution_> sourceVariableDescriptor;

    private static final int NEVER_QUIT_EARLY = Integer.MAX_VALUE;

    public IndexVariableListener(
            IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor,
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
        updateIndexes((InnerScoreDirector<Solution_, ?>) scoreDirector, entity, 0, NEVER_QUIT_EARLY);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        List<Object> listVariable = sourceVariableDescriptor.getValue(entity);
        for (Object element : listVariable) {
            innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
            shadowVariableDescriptor.setValue(element, null);
            innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
        }
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object element) {
        InnerScoreDirector<Solution_, ?> innerScoreDirector = (InnerScoreDirector<Solution_, ?>) scoreDirector;
        innerScoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
        shadowVariableDescriptor.setValue(element, null);
        innerScoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
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
        updateIndexes(innerScoreDirector, entity, fromIndex, toIndex);
    }

    private void updateIndexes(InnerScoreDirector<Solution_, ?> scoreDirector, Object entity, int fromIndex, int toIndex) {
        List<Object> listVariable = sourceVariableDescriptor.getValue(entity);
        for (int i = fromIndex; i < listVariable.size(); i++) {
            Object element = listVariable.get(i);
            Integer oldIndex = shadowVariableDescriptor.getValue(element);
            if (!Objects.equals(oldIndex, i)) {
                scoreDirector.beforeVariableChanged(shadowVariableDescriptor, element);
                shadowVariableDescriptor.setValue(element, i);
                scoreDirector.afterVariableChanged(shadowVariableDescriptor, element);
            } else if (i >= toIndex) {
                // Do not quit early while inside the affected subList range.
                // Example 1. When X is moved from Ann[3] to Beth[3], we need to start updating Beth's elements at index 3
                // where X already has the expected index, but quitting there would be incorrect because all the elements
                // above X need their indexes incremented.
                // Example 2. After ListSwapMove(Ann, 5, 9), the listener must not quit at index 6, but it can quit at index 10.
                return;
            }
        }
    }

    @Override
    public Integer getIndex(Object planningValue) {
        return shadowVariableDescriptor.getValue(planningValue);
    }
}
