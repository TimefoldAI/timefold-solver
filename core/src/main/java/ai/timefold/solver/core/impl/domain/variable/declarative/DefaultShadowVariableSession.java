package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.Supply;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class DefaultShadowVariableSession<Solution_> implements Supply {

    final VariableReferenceGraph<Solution_> graph;

    public DefaultShadowVariableSession(VariableReferenceGraph<Solution_> graph) {
        this.graph = graph;
    }

    public void beforeVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        graph.beforeVariableChanged(variableDescriptor.getVariableMetaModel(),
                entity);
    }

    public void afterVariableChanged(VariableDescriptor<Solution_> variableDescriptor, Object entity) {
        graph.afterVariableChanged(variableDescriptor.getVariableMetaModel(),
                entity);
    }

    public void updateVariables() {
        graph.updateChanged();
    }
}
