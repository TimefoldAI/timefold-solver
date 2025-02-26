package ai.timefold.solver.core.impl.domain.variable.declarative;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableProvider;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSession;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariableSessionFactory;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class MockShadowVariableSessionFactory<Solution_> implements ShadowVariableSessionFactory {
    final ShadowVariableProvider shadowVariableProvider;
    final SolutionDescriptor<Solution_> solutionDescriptor;

    public MockShadowVariableSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ShadowVariableProvider shadowVariableProvider) {
        this.shadowVariableProvider = shadowVariableProvider;
        this.solutionDescriptor = solutionDescriptor;
    }

    @Override
    public ShadowVariableSession forEntities(Object... entities) {
        var stateSupply = new MockListStateSupply<>(solutionDescriptor.getListVariableDescriptor());
        var shadowVariableFactory = new DefaultShadowVariableFactory<>(solutionDescriptor,
                new MockSupplyManager(stateSupply));
        var variableReferenceGraph = new VariableReferenceGraph<Solution_>(ChangedVariableNotifier.empty());
        shadowVariableProvider.defineVariables(shadowVariableFactory);

        DefaultShadowVariableSessionFactory.visitGraph(shadowVariableFactory, variableReferenceGraph, entities,
                DefaultTopologicalOrderGraph::new);

        return new MockShadowVariableSession<>(solutionDescriptor, variableReferenceGraph, stateSupply);
    }
}
