package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.IdentityHashMap;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class MockListStateSupply<Solution_> implements ListVariableStateSupply<Solution_> {
    final ListVariableDescriptor<Solution_> sourceVariableDescriptor;
    IdentityHashMap<Object, Object> previousMap = new IdentityHashMap<>();
    IdentityHashMap<Object, Object> nextMap = new IdentityHashMap<>();
    IdentityHashMap<Object, Object> inverseMap = new IdentityHashMap<>();
    IdentityHashMap<Object, Integer> indexMap = new IdentityHashMap<>();

    public MockListStateSupply(ListVariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    public void setPrevious(Object object, @Nullable Object previous) {
        previousMap.put(object, previous);
        if (previous != null) {
            nextMap.put(previous, object);
        }
    }

    public void setNext(Object object, @Nullable Object next) {
        nextMap.put(object, next);
        if (next != null) {
            previousMap.put(next, object);
        }
    }

    public void setInverse(Object object, @Nullable Object inverse) {
        inverseMap.put(object, inverse);
    }

    public void setIndex(Object object, @Nullable Integer index) {
        indexMap.put(object, index);
    }

    @Override
    public ListVariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    @Nullable
    public Object getPreviousElement(Object element) {
        return previousMap.get(element);
    }

    @Override
    @Nullable
    public Object getNextElement(Object element) {
        return nextMap.get(element);
    }

    @Override
    @Nullable
    public Integer getIndex(Object planningValue) {
        return indexMap.get(planningValue);
    }

    @Override
    @Nullable
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
    public void afterListVariableElementUnassigned(ScoreDirector<Solution_> scoreDirector, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object object,
            int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object object, int fromIndex,
            int toIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object object) {
        throw new UnsupportedOperationException();
    }
}
