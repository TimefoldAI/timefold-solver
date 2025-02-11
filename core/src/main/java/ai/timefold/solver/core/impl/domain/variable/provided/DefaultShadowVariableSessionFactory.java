package ai.timefold.solver.core.impl.domain.variable.provided;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableProvider;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableSession;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableSessionFactory;

public class DefaultShadowVariableSessionFactory<Solution_> implements ShadowVariableSessionFactory {
    final ShadowVariableProvider shadowVariableProvider;
    final SolutionDescriptor<Solution_> solutionDescriptor;

    public DefaultShadowVariableSessionFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ShadowVariableProvider shadowVariableProvider) {
        this.shadowVariableProvider = shadowVariableProvider;
        this.solutionDescriptor = solutionDescriptor;
    }

    @Override
    public ShadowVariableSession forEntities(Object... entities) {
        var stateSupply = new MockListStateSupply<>(solutionDescriptor.getListVariableDescriptor());
        var shadowVariableFactory = new DefaultShadowVariableFactory<>(solutionDescriptor,
                new MockSupplyManager(stateSupply));
        var variableReferenceGraph = new VariableReferenceGraph(ChangedVariableNotifier.empty());
        shadowVariableProvider.defineVariables(shadowVariableFactory);
        for (var groupReference : shadowVariableFactory.getGroupVariableReferenceList()) {
            for (var entity : entities) {
                groupReference.processGroupElements(variableReferenceGraph, groupReference, entity);
            }
        }
        for (var shadowVariable : shadowVariableFactory.getShadowVariableReferenceList()) {
            shadowVariable.visitGraph(variableReferenceGraph);
        }
        for (var entity : entities) {
            for (var shadowVariable : shadowVariableFactory.getShadowVariableReferenceList()) {
                shadowVariable.visitEntity(variableReferenceGraph, entity);
            }
        }
        variableReferenceGraph.createGraph(DefaultTopologicalOrderGraph::new);
        return new MockShadowVariableSession<>(solutionDescriptor, variableReferenceGraph, stateSupply);
    }
}
