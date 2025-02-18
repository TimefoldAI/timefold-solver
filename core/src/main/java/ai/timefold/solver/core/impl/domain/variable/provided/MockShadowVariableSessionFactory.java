package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableProvider;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableSession;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableSessionFactory;

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

        DefaultShadowVariableSessionFactory.visitGraph(shadowVariableFactory, variableReferenceGraph, entities);

        return new MockShadowVariableSession<>(solutionDescriptor, variableReferenceGraph, stateSupply);
    }
}
