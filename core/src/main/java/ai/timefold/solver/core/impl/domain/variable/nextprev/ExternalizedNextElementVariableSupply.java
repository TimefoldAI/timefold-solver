package ai.timefold.solver.core.impl.domain.variable.nextprev;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.variable.ListVariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.util.IndexedElementAwareList;

/**
 * Alternative to {@link NextElementVariableListener}.
 */
public class ExternalizedNextElementVariableSupply<Solution_> implements
        SourcedVariableListener<Solution_>,
        ListVariableListener<Solution_, Object, Object>,
        NextElementVariableSupply {

    private final ListVariableDescriptor<Solution_> sourceListVariableDescriptor;
    private final SingletonInverseVariableSupply inverseVariableSupply;
    private Map<Object, IndexedElementAwareList<Object>> nextElementListMap = null;

    public ExternalizedNextElementVariableSupply(ListVariableDescriptor<Solution_> sourceListVariableDescriptor,
            SingletonInverseVariableSupply inverseVariableSupply) {
        this.sourceListVariableDescriptor = sourceListVariableDescriptor;
        this.inverseVariableSupply = inverseVariableSupply;
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
        valueList.forEach(value -> retract(entity, value));
    }

    @Override
    public void afterListVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity, int fromIndex, int toIndex) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity);
        var previous = fromIndex == 0 ? null : valueList.get(fromIndex - 1);
        for (int i = fromIndex; i < toIndex; i++) {
            if (previous != null) {
                insertAfter(entity, valueList.get(i), previous);
            } else {
                insertFirst(entity, valueList.get(i));
            }
            previous = valueList.get(i);
        }
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity);
        valueList.forEach(value -> retract(entity, value));
    }

    @Override
    public void afterListVariableElementUnassigned(ScoreDirector<Solution_> scoreDirector, Object value) {
        // Do nothing
    }

    @Override
    public void afterEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        // Do nothing
    }

    private Object getInverseRelationEntity(Object entity) {
        return inverseVariableSupply.getInverseSingleton(entity);
    }

    private void insertAll(Object entity) {
        List<Object> valueList = sourceListVariableDescriptor.getValue(entity);
        nextElementListMap.put(entity, new IndexedElementAwareList<>());
        for (Object value : valueList) {
            insert(entity, value);
        }
    }

    private void insert(Object entity, Object value) {
        var elementList = nextElementListMap.computeIfAbsent(entity, e -> new IndexedElementAwareList<>());
        var addSucceeded = elementList.add(value);
        if (!addSucceeded) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceListVariableDescriptor.getVariableName()
                    + ") cannot be inserted: it was already inserted.");
        }
    }

    private void insertFirst(Object entity, Object value) {
        var elementList = nextElementListMap.computeIfAbsent(entity, e -> new IndexedElementAwareList<>());
        var addSucceeded = elementList.addFirst(value);
        if (!addSucceeded) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceListVariableDescriptor.getVariableName()
                    + ") cannot be inserted: it was already inserted.");
        }
    }

    private void insertAfter(Object entity, Object value, Object previous) {
        var elementList = nextElementListMap.computeIfAbsent(entity, e -> new IndexedElementAwareList<>());
        var addSucceeded = elementList.addAfter(value, previous);
        if (!addSucceeded) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceListVariableDescriptor.getVariableName()
                    + ") cannot be inserted: it was already inserted.");
        }
    }

    private void retract(Object entity, Object value) {
        var elementList = nextElementListMap.get(entity);
        if (elementList == null && !sourceListVariableDescriptor.allowsUnassignedValues()) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceListVariableDescriptor.getVariableName()
                    + ") cannot be retracted: it was never inserted.");
        }
        if (elementList != null && !elementList.remove(value)) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceListVariableDescriptor.getVariableName()
                    + ") cannot be retracted: it was never inserted.");
        }
    }

    @Override
    public Object getNext(Object planningValue) {
        Object entity = getInverseRelationEntity(planningValue);
        Object nextValue = null;
        if (nextElementListMap.containsKey(entity)) {
            nextValue = nextElementListMap.get(entity).next(planningValue);
        }
        return nextValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceListVariableDescriptor.getVariableName() + ")";
    }
}
