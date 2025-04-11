package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSession;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class MockShadowVariableSession<Solution_> implements ShadowVariableSession {
    final SolutionDescriptor<Solution_> solutionDescriptor;
    final VariableReferenceGraph<Solution_> graph;

    public MockShadowVariableSession(SolutionDescriptor<Solution_> solutionDescriptor,
            VariableReferenceGraph<Solution_> graph) {
        this.solutionDescriptor = solutionDescriptor;
        this.graph = graph;
    }

    @Override
    public void setVariable(Object entity, String variableName, @Nullable Object value) {
        var variableId = new VariableId(entity.getClass(), variableName);
        graph.beforeVariableChanged(variableId, entity);
        solutionDescriptor.getEntityDescriptorStrict(entity.getClass()).getVariableDescriptor(variableName)
                .setValue(entity, value);
        graph.afterVariableChanged(variableId, entity);
    }

    @Override
    public void updateVariables() {
        graph.updateChanged();
    }
}
