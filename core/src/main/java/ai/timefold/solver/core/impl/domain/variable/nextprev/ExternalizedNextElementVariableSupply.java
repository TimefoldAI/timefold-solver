package ai.timefold.solver.core.impl.domain.variable.nextprev;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.heuristic.selector.list.NextPreviousInList;

/**
 * Alternative to {@link NextElementVariableListener}.
 */
public class ExternalizedNextElementVariableSupply<Solution_> implements
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        NextElementVariableSupply {

    private final ListVariableDescriptor<Solution_> sourceListVariableDescriptor;
    private Map<Object, NextPreviousInList> nextElementListMap = null;

    public ExternalizedNextElementVariableSupply(ListVariableDescriptor<Solution_> sourceListVariableDescriptor) {
        this.sourceListVariableDescriptor = sourceListVariableDescriptor;
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceListVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(ScoreDirector<Solution_> scoreDirector) {
        nextElementListMap = new IdentityHashMap<>();
        sourceListVariableDescriptor.getEntityDescriptor().visitAllEntities(scoreDirector.getWorkingSolution(),
                this::insertAll);
    }

    @Override
    public void close() {
        nextElementListMap = null;
    }

    @Override
    public void beforeEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(ScoreDirector<Solution_> scoreDirector, Object entity) {
        insertAll(entity);
    }

    @Override
    public void beforeListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity).subList(fromIndex, toIndex);
        valueList.forEach(this::retract);
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity);
        var next = toIndex < valueList.size() ? nextElementListMap.get(valueList.get(toIndex)) : null;
        for (int i = toIndex - 1; i >= fromIndex; i--) {
            next = insertBefore(valueList.get(i), next != null ? next.getTuple() : null);
        }
        if (next != null && next.getPrevious() == null && fromIndex > 0) {
            // When adding a partial set, they need to be connected (KOpt moves)
            var previousElement = nextElementListMap.get(valueList.get(fromIndex - 1));
            next.setPrevious(previousElement);
            previousElement.setNext(next);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity);
        valueList.forEach(this::retract);
    }

    @Override
    public void afterListVariableElementUnassigned(ScoreDirector<Solution_> scoreDirector, Object value) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    private void insertAll(Object entity) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity);
        NextPreviousInList previous = null;
        for (Object value : valueList) {
            previous = insertAfter(value, previous != null ? previous.getTuple() : null);
        }
    }

    private NextPreviousInList insertBefore(Object value, Object nextValue) {
        if (nextElementListMap.containsKey(value)) {
            throw new IllegalStateException(
                    "The supply (%s) is corrupted, because the entity (%s) for sourceVariable (%s) cannot be inserted: it was already inserted."
                            .formatted(this, value, sourceListVariableDescriptor.getVariableName()));
        }
        var next = nextElementListMap.get(nextValue);
        var newElement = new NextPreviousInList(value, next);
        if (next != null) {
            next.setPrevious(newElement);
        }
        nextElementListMap.put(value, newElement);
        return newElement;
    }

    private NextPreviousInList insertAfter(Object value, Object previousValue) {
        if (nextElementListMap.containsKey(value)) {
            throw new IllegalStateException(
                    "The supply (%s) is corrupted, because the entity (%s) for sourceVariable (%s) cannot be inserted: it was already inserted."
                            .formatted(this, value, sourceListVariableDescriptor.getVariableName()));
        }
        var previous = nextElementListMap.get(previousValue);
        NextPreviousInList next = null;
        if (previous != null) {
            next = previous.getNext();
        }
        var newElement = new NextPreviousInList(value, previous, next);
        if (previous != null && next != null) {
            previous.setNext(newElement);
            next.setPrevious(newElement);
        } else if (previous != null) {
            previous.setNext(newElement);
        }
        if (next != null) {
            next.setPrevious(newElement);
        }
        nextElementListMap.put(value, newElement);
        return newElement;
    }

    private void retract(Object value) {
        var element = nextElementListMap.remove(value);
        if (element == null && !sourceListVariableDescriptor.allowsUnassignedValues()) {
            throw new IllegalStateException(
                    "The supply (%s) is corrupted, because the entity (%s) for sourceVariable (%s) cannot be retracted: it was never inserted."
                            .formatted(this, value, sourceListVariableDescriptor.getVariableName()));
        }
        if (element != null) {
            var previous = element.getPrevious();
            var next = element.getNext();
            if (previous != null && next != null) {
                previous.setNext(next);
                next.setPrevious(previous);
            } else if (previous != null) {
                previous.setNext(null);
            } else if (next != null) {
                next.setPrevious(null);
            }
        }
    }

    @Override
    public Object getNext(Object planningValue) {
        var element = nextElementListMap.get(planningValue);
        Object next = null;
        if (element != null && element.getNext() != null) {
            next = element.getNext().getTuple();
        }
        return next;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceListVariableDescriptor.getVariableName() + ")";
    }
}
