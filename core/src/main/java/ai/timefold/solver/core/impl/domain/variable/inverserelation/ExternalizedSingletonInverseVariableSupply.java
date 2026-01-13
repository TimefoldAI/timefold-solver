package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedBasicVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

/**
 * Alternative to {@link SingletonInverseVariableListener}.
 */
public class ExternalizedSingletonInverseVariableSupply<Solution_> implements
        SourcedBasicVariableListener<Solution_, Object>,
        SingletonInverseVariableSupply {

    protected final VariableDescriptor<Solution_> sourceVariableDescriptor;
    private final Consumer<Object> notifier;

    protected Map<Object, Object> inverseEntityMap = null;

    public ExternalizedSingletonInverseVariableSupply(VariableDescriptor<Solution_> sourceVariableDescriptor,
            Consumer<Object> notifier) {
        this.sourceVariableDescriptor = sourceVariableDescriptor;
        this.notifier = notifier;
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return sourceVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(@NonNull InnerScoreDirector<Solution_, ?> scoreDirector) {
        inverseEntityMap = new IdentityHashMap<>();
        sourceVariableDescriptor.getEntityDescriptor().visitAllEntities(scoreDirector.getWorkingSolution(), this::insert);
    }

    @Override
    public void close() {
        inverseEntityMap = null;
    }

    @Override
    public void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        retract(event.entity());
    }

    @Override
    public void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        insert(event.entity());
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
        notifier.accept(value);
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
        notifier.accept(value);
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
