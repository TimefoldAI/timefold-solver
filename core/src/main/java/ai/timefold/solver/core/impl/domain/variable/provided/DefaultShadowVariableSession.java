package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableSession;

public class DefaultShadowVariableSession<Solution_> implements ShadowVariableSession, Supply {
    final VariableReferenceGraph<Solution_> graph;

    public DefaultShadowVariableSession(VariableReferenceGraph<Solution_> graph) {
        this.graph = graph;
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
        graph.beforeVariableChanged(VariableId.entity(entityClass).child(entityClass, variableDescriptor.getVariableName()),
                entity);
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        var entityClass = variableDescriptor.getEntityDescriptor().getEntityClass();
        graph.afterVariableChanged(VariableId.entity(entityClass).child(entityClass, variableDescriptor.getVariableName()),
                entity);
    }

    public void beforeListElementChanged(Object entity) {
        var entityClass = entity.getClass();
        var entityRootId = VariableId.entity(entityClass);
        var inverseVariableId = entityRootId.child(entityClass, DefaultShadowVariableFactory.INVERSE);
        var previousVariableId = entityRootId.child(entityClass, DefaultShadowVariableFactory.PREVIOUS);
        var nextVariableId = entityRootId.child(entityClass, DefaultShadowVariableFactory.NEXT);

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
        var inverseVariableId = entityRootId.child(entityClass, DefaultShadowVariableFactory.INVERSE);
        var previousVariableId = entityRootId.child(entityClass, DefaultShadowVariableFactory.PREVIOUS);
        var nextVariableId = entityRootId.child(entityClass, DefaultShadowVariableFactory.NEXT);

        graph.afterVariableChanged(inverseVariableId,
                entity);
        graph.afterVariableChanged(previousVariableId,
                entity);
        graph.afterVariableChanged(nextVariableId,
                entity);
    }

    public void afterListElementUnassigned(Object entity) {
    }

    @Override
    public void updateVariables() {
        graph.updateChanged();
    }

    // Unsupported methods
    @Override
    public void setVariable(Object entity, String variableName, Object value) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }

    @Override
    public void setPrevious(Object entity, Object previousValue) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }

    @Override
    public void setNext(Object entity, Object nextValue) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }

    @Override
    public void setInverse(Object entity, Object inverseValue) {
        throw new UnsupportedOperationException("Use before and after methods instead");
    }
}
