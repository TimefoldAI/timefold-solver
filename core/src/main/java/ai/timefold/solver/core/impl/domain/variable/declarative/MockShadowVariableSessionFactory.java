package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSession;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSessionFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class MockShadowVariableSessionFactory<Solution_> implements ShadowVariableSessionFactory {
    final SolutionDescriptor<Solution_> solutionDescriptor;

    public MockShadowVariableSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = solutionDescriptor;
    }

    @Override
    public ShadowVariableSession forEntities(Object... entities) {
        var variableReferenceGraph = new VariableReferenceGraph<Solution_>(ChangedVariableNotifier.empty());

        DefaultShadowVariableSessionFactory.visitGraph(solutionDescriptor, variableReferenceGraph, entities,
                DefaultTopologicalOrderGraph::new);

        return new MockShadowVariableSession<>(solutionDescriptor, variableReferenceGraph);
    }
}
