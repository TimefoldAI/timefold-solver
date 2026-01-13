package ai.timefold.solver.core.impl.domain.variable.anchor;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.domain.variable.listener.SourcedBasicVariableListener;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Alternative to {@link AnchorVariableListener}.
 */
@NullMarked
public class ExternalizedAnchorVariableSupply<Solution_> implements
        SourcedBasicVariableListener<Solution_, Object>,
        AnchorVariableSupply {

    protected final VariableDescriptor<Solution_> previousVariableDescriptor;
    protected final SingletonInverseVariableSupply nextVariableSupply;
    protected final Map<Object, @Nullable Object> anchorMap;
    private final Consumer<Object> notifier;

    public ExternalizedAnchorVariableSupply(VariableDescriptor<Solution_> previousVariableDescriptor,
            SingletonInverseVariableSupply nextVariableSupply,
            Consumer<Object> notifier) {
        this.previousVariableDescriptor = previousVariableDescriptor;
        this.nextVariableSupply = nextVariableSupply;
        this.anchorMap = new IdentityHashMap<>();
        this.notifier = notifier;
    }

    @Override
    public VariableDescriptor<Solution_> getSourceVariableDescriptor() {
        return previousVariableDescriptor;
    }

    @Override
    public void resetWorkingSolution(InnerScoreDirector<Solution_, ?> scoreDirector) {
        anchorMap.clear();
        previousVariableDescriptor.getEntityDescriptor().visitAllEntities(scoreDirector.getWorkingSolution(), this::insert);
    }

    @Override
    public void beforeChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        // No need to retract() because the insert (which is guaranteed to be called later) affects the same trailing entities.
    }

    @Override
    public void afterChange(InnerScoreDirector<Solution_, ?> scoreDirector,
            BasicVariableChangeEvent<Object> event) {
        insert(event.entity());
    }

    @Override
    public void close() {
        anchorMap.clear();
    }

    protected void insert(Object entity) {
        Object previousEntity = previousVariableDescriptor.getValue(entity);
        Object anchor;
        if (previousEntity == null) {
            anchor = null;
        } else if (previousVariableDescriptor.isValuePotentialAnchor(previousEntity)) {
            anchor = previousEntity;
        } else {
            anchor = anchorMap.get(previousEntity);
        }
        Object nextEntity = entity;
        while (nextEntity != null && anchorMap.get(nextEntity) != anchor) {
            anchorMap.put(nextEntity, anchor);
            notifier.accept(nextEntity);
            nextEntity = nextVariableSupply.getInverseSingleton(nextEntity);
        }
    }

    @Override
    public @Nullable Object getAnchor(Object entity) {
        return anchorMap.get(entity);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + previousVariableDescriptor.getVariableName() + ")";
    }

}
