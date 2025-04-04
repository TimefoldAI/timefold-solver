package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSession;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DefaultShadowVariableSession<Solution_> implements ShadowVariableSession, Supply {
    final VariableReferenceGraph<Solution_> graph;

    public DefaultShadowVariableSession(VariableReferenceGraph<Solution_> graph) {
        this.graph = graph;
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
        graph.beforeVariableChanged(VariableId.entity(entityClass).child(variableDescriptor.getVariableName()),
                entity);
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
        graph.afterVariableChanged(VariableId.entity(entityClass).child(variableDescriptor.getVariableName()),
                entity);
    }

    public void beforeListElementChanged(Object entity) {
        var entityClass = entity.getClass();
        var entityRootId = VariableId.entity(entityClass);
        var inverseVariableId = entityRootId.child(DefaultShadowVariableFactory.INVERSE);
        var previousVariableId = entityRootId.child(DefaultShadowVariableFactory.PREVIOUS);
        var nextVariableId = entityRootId.child(DefaultShadowVariableFactory.NEXT);

        graph.beforeVariableChanged(inverseVariableId,
                entity);
        graph.beforeVariableChanged(previousVariableId,
                entity);
        graph.beforeVariableChanged(nextVariableId,
                entity);
    }

    public void afterListElementChanged(Object entity) {
        var entityClass = entity.getClass();
        var entityRootId = VariableId.entity(entityClass);
        var inverseVariableId = entityRootId.child(DefaultShadowVariableFactory.INVERSE);
        var previousVariableId = entityRootId.child(DefaultShadowVariableFactory.PREVIOUS);
        var nextVariableId = entityRootId.child(DefaultShadowVariableFactory.NEXT);

        graph.afterVariableChanged(inverseVariableId,
                entity);
        graph.afterVariableChanged(previousVariableId,
                entity);
        graph.afterVariableChanged(nextVariableId,
                entity);
    }

    public void afterListElementUnassigned(Object entity) {
        var entityClass = entity.getClass();
        var entityRootId = VariableId.entity(entityClass);
        var inverseVariableId = graph.lookup(entityRootId.child(DefaultShadowVariableFactory.INVERSE),
                entity);
        var previousVariableId = graph.lookup(entityRootId.child(DefaultShadowVariableFactory.PREVIOUS),
                entity);
        var nextVariableId = graph.lookup(entityRootId.child(DefaultShadowVariableFactory.NEXT),
                entity);

        if (inverseVariableId != null) {
            graph.markChanged(inverseVariableId);
        }
        if (previousVariableId != null) {
            graph.markChanged(previousVariableId);
        }
        if (nextVariableId != null) {
            graph.markChanged(nextVariableId);
        }
    }

    @Override
    public void updateVariables() {
        graph.updateChanged();
    }

    // Unsupported methods
    @Override
    public void setVariable(Object entity, String variableName, @Nullable Object value) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }

    @Override
    public void setPrevious(Object entity, @Nullable Object previousValue) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }

    @Override
    public void setNext(Object entity, @Nullable Object nextValue) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }

    @Override
    public void setInverse(Object entity, @Nullable Object inverseValue) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }
}
