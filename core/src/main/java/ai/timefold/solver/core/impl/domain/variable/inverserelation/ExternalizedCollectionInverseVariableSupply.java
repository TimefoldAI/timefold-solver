package ai.timefold.solver.core.impl.domain.variable.inverserelation;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedBasicVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NonNull;

/**
 * Alternative to {@link CollectionInverseVariableListener}.
 */
public class ExternalizedCollectionInverseVariableSupply<Solution_> implements
        SourcedBasicVariableListener<Solution_, Object>,
        CollectionInverseVariableSupply {

    protected final VariableDescriptor<Solution_> sourceVariableDescriptor;
    protected final Consumer<Object> notifier;

    protected Map<Object, Set<Object>> inverseEntitySetMap = null;

    public ExternalizedCollectionInverseVariableSupply(VariableDescriptor<Solution_> sourceVariableDescriptor,
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
        inverseEntitySetMap = new IdentityHashMap<>();
        sourceVariableDescriptor.getEntityDescriptor().visitAllEntities(scoreDirector.getWorkingSolution(), this::insert);
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

    @Override
    public void close() {
        inverseEntitySetMap = null;
    }

    protected void insert(Object entity) {
        Object value = sourceVariableDescriptor.getValue(entity);
        if (value == null) {
            return;
        }
        Set<Object> inverseEntitySet = inverseEntitySetMap.computeIfAbsent(value,
                k -> Collections.newSetFromMap(new IdentityHashMap<>()));
        boolean addSucceeded = inverseEntitySet.add(entity);
        if (!addSucceeded) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceVariableDescriptor.getVariableName()
                    + ") cannot be inserted: it was already inserted.");
        }
        notifier.accept(value);
    }

    protected void retract(Object entity) {
        Object value = sourceVariableDescriptor.getValue(entity);
        if (value == null) {
            return;
        }
        Set<Object> inverseEntitySet = inverseEntitySetMap.get(value);
        boolean removeSucceeded = inverseEntitySet.remove(entity);
        if (!removeSucceeded) {
            throw new IllegalStateException("The supply (" + this + ") is corrupted,"
                    + " because the entity (" + entity
                    + ") for sourceVariable (" + sourceVariableDescriptor.getVariableName()
                    + ") cannot be retracted: it was never inserted.");
        }
        if (inverseEntitySet.isEmpty()) {
            inverseEntitySetMap.put(value, null);
        }
        notifier.accept(value);
    }

    @Override
    public Collection<?> getInverseCollection(Object value) {
        Set<Object> inverseEntitySet = inverseEntitySetMap.get(value);
        if (inverseEntitySet == null) {
            return Collections.emptySet();
        }
        return inverseEntitySet;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceVariableDescriptor.getVariableName() + ")";
    }

}
