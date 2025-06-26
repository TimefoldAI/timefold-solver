package ai.timefold.solver.core.impl.domain.variable.declarative;

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

    public void updateVariables() {
        graph.updateChanged();
    }
}
