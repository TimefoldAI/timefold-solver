package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.IdentityHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.domain.variable.VariableListener;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedVariableListener;

import org.jspecify.annotations.NonNull;

/**
 * Alternative to {@link SingletonInverseVariableListener}.
 */
public class ExternalizedSingletonInverseVariableSupply<Solution_> implements
        SourcedVariableListener<Solution_>,
        VariableListener<Solution_, Object>,
        SingletonInverseVariableSupply {

    protected final VariableDescriptor<Solution_> sourceVariableDescriptor;

    protected Map<Object, Object> inverseEntityMap = null;

    public ExternalizedSingletonInverseVariableSupply(VariableDescriptor<Solution_> sourceVariableDescriptor) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(@NonNull ScoreDirector<Solution_> scoreDirector) {
        inverseEntityMap = new IdentityHashMap<>();
        sourceVariableDescriptor.getEntityDescriptor().visitAllEntities(scoreDirector.getWorkingSolution(), this::insert);
    }

    @Override
    public void close() {
        inverseEntityMap = null;
    }

    @Override
    public void beforeEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    @Override
    public void afterEntityAdded(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        insert(entity);
    }

    @Override
    public void beforeVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        retract(entity);
    }

    @Override
    public void afterVariableChanged(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        insert(entity);
    }

    @Override
    public void beforeEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        retract(entity);
    }

    @Override
    public void afterEntityRemoved(@NonNull ScoreDirector<Solution_> scoreDirector, @NonNull Object entity) {
        // Do nothing
    }

    protected void insert(Object entity) {
        Object value = sourceVariableDescriptor.getValue(entity);
        if (value == null) {
            return;
        }
        Object oldInverseEntity = inverseEntityMap.put(value, entity);
        if (oldInverseEntity != null) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceVariableDescriptor.getVariableName()
                    + ") cannot be inserted: another entity (" + oldInverseEntity
                    + ") already has that value (" + value + ").");
        }
    }

    protected void retract(Object entity) {
        Object value = sourceVariableDescriptor.getValue(entity);
        if (value == null) {
            return;
        }
        Object oldInverseEntity = inverseEntityMap.remove(value);
        if (oldInverseEntity != entity) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceVariableDescriptor.getVariableName()
                    + ") cannot be retracted: the entity was never inserted for that value (" + value + ").");
        }
    }

    @Override
    public Object getInverseSingleton(Object value) {
        return inverseEntityMap.get(value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceVariableDescriptor.getVariableName() + ")";
    }

}
