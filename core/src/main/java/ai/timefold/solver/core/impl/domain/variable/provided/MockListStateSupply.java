package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.IdentityHashMap;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;

import org.jspecify.annotations.NonNull;

public class MockListStateSupply<Solution_> implements ListVariableStateSupply<Solution_> {
    final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    IdentityHashMap<Object, Object> previousMap = new IdentityHashMap<>();
    IdentityHashMap<Object, Object> nextMap = new IdentityHashMap<>();
    IdentityHashMap<Object, Object> inverseMap = new IdentityHashMap<>();
    IdentityHashMap<Object, Integer> indexMap = new IdentityHashMap<>();

    public MockListStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    public void setPrevious(Object object, Object previous) {
        previousMap.put(object, previous);
        if (previous != null) {
            nextMap.put(previous, object);
        }
    }

    public void setNext(Object object, Object next) {
        nextMap.put(object, next);
        if (next != null) {
            previousMap.put(next, object);
        }
    }

    public void setInverse(Object object, Object inverse) {
        inverseMap.put(object, inverse);
    }

    public void setIndex(Object object, Integer index) {
        indexMap.put(object, index);
    }

    @Override
    public ListVariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    public Object getPreviousElement(Object element) {
        return previousMap.get(element);
    }

    @Override
    public Object getNextElement(Object element) {
        return nextMap.get(element);
    }

    @Override
    public Integer getIndex(Object planningValue) {
        return indexMap.get(planningValue);
    }

    @Override
    public Object getInverseSingleton(Object planningValue) {
        return inverseMap.get(planningValue);
    }

    // Unsupported operations
    @Override
    public void externalize(IndexShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void externalize(InverseRelationShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void externalize(PreviousElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void externalize(NextElementShadowVariableDescriptor<Solution_> shadowVariableDescriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssigned(Object element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ElementLocation getLocationInList(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUnassignedCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterListVariableElementUnassigned(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object,
            int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterListVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object, int fromIndex,
            int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object object) {
        throw new UnsupportedOperationException();
    }
}
