package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultShadowVariableSession<Solution_> implements Supply {
    final VariableReferenceGraph graph;

    public DefaultShadowVariableSession(VariableReferenceGraph graph) {
        this.graph = graph;
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        beforeVariableChanged(variableDescriptor.getVariableMetaModel(),
                entity);
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        afterVariableChanged(variableDescriptor.getVariableMetaModel(),
                entity);
    }

    public void beforeVariableChanged(VariableMetaModel<Solution_, ?, ?> variableMetaModel, Object entity) {
        graph.beforeVariableChanged(variableMetaModel,
                entity);
    }

    public void afterVariableChanged(VariableMetaModel<Solution_, ?, ?> variableMetaModel, Object entity) {
        graph.afterVariableChanged(variableMetaModel,
                entity);
    }

    /**
     * @return true if the graph listens to the given list variable, false if the event was a no-op.
     */
    public boolean beforeListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity,
            int fromIndex, int toIndex) {
        var variableMetaModel = variableDescriptor.getVariableMetaModel();
        if (!graph.requiresListVariableChangeEvents(variableMetaModel)) {
            return false;
        }
        graph.beforeListVariableChanged(variableMetaModel, entity,
                variableDescriptor.getValue(entity), fromIndex, toIndex);
        return true;
    }

    public void afterListVariableChanged(ListVariableDescriptor<Solution_> variableDescriptor, Object entity, int fromIndex,
            int toIndex) {
        var variableMetaModel = variableDescriptor.getVariableMetaModel();
        if (!graph.requiresListVariableChangeEvents(variableMetaModel)) {
            return;
        }
        graph.afterListVariableChanged(variableMetaModel, entity,
                variableDescriptor.getValue(entity), fromIndex, toIndex);
    }

    public void updateVariables() {
        graph.updateChanged();
    }
}
