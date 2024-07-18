package ai.timefold.solver.core.impl.domain.variable.nextprev;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;
import ai.timefold.solver.core.impl.util.IdentityElementAwareList;

/**
 * Alternative to {@link NextElementVariableListener}.
 */
public class ExternalizedNextElementVariableSupply<Solution_> implements
        SourcedVariableListener<Solution_>,
        VariableListener<Solution_, Object>,
        NextElementVariableSupply {

    private final ListVariableDescriptor<Solution_> sourceListVariableDescriptor;
    private final SingletonInverseVariableSupply inverseVariableSupply;
    private Map<Object, IdentityElementAwareList<Object>> nextElementListMap = null;

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
        insert(getInverseRelationEntity(entity), entity);
    }

    @Override
    public void beforeVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        retract(getInverseRelationEntity(entity), entity);
    }

    @Override
    public void afterVariableChanged(ScoreDirector<Solution_> scoreDirector, Object entity) {
        insert(getInverseRelationEntity(entity), entity);
    }

    @Override
    public void beforeEntityRemoved(ScoreDirector<Solution_> scoreDirector, Object entity) {
        retract(getInverseRelationEntity(entity), entity);
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
        nextElementListMap.put(entity, new IdentityElementAwareList<>());
        for (Object value : valueList) {
            insert(entity, value);
        }
    }

    private void insert(Object entity, Object value) {
        var elementList = nextElementListMap.computeIfAbsent(entity, e -> new IdentityElementAwareList<>());
        var addSucceeded = elementList.add(value);
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
        Object element = null;
        if (nextElementListMap.containsKey(entity)) {
            element = nextElementListMap.get(entity).next(planningValue);
        }
        return element;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceListVariableDescriptor.getVariableName() + ")";
    }

}
