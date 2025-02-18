package ai.timefold.solver.core.impl.domain.variable.provided;

import java.util.ArrayList;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableProvider;
import ai.timefold.solver.core.preview.api.variable.provided.ShadowVariableSessionFactory;

public class DefaultShadowVariableSessionFactory<Solution_> implements ShadowVariableSessionFactory {
    private final Set<ShadowVariableProvider> shadowVariableProviderSet;
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final InnerScoreDirector<Solution_, ?> scoreDirector;
    private final SupplyManager supplyManager;

    public DefaultShadowVariableSessionFactory(Set<ShadowVariableProvider> shadowVariableProviderSet,
            SolutionDescriptor<Solution_> solutionDescriptor,
            InnerScoreDirector<Solution_, ?> scoreDirector, SupplyManager supplyManager) {
        this.shadowVariableProviderSet = shadowVariableProviderSet;
        this.solutionDescriptor = solutionDescriptor;
        this.scoreDirector = scoreDirector;
        this.supplyManager = supplyManager;
    }

    static <Solution_> void visitGraph(DefaultShadowVariableFactory<Solution_> shadowVariableFactory,
            VariableReferenceGraph<Solution_> variableReferenceGraph, Object[] entities) {
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
    }

    public DefaultShadowVariableSession<Solution_> forSolution(Solution_ solution) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        return forEntities(entities.toArray());
    }

    @Override
    public DefaultShadowVariableSession<Solution_> forEntities(Object... entities) {
        var shadowVariableFactory = new DefaultShadowVariableFactory<>(solutionDescriptor, supplyManager);
        var variableReferenceGraph = new VariableReferenceGraph<>(ChangedVariableNotifier.of(scoreDirector));
        for (var shadowVariableProvider : shadowVariableProviderSet) {
            shadowVariableProvider.defineVariables(shadowVariableFactory);
        }

        visitGraph(shadowVariableFactory, variableReferenceGraph, entities);

        return new DefaultShadowVariableSession<>(variableReferenceGraph);
    }
}
