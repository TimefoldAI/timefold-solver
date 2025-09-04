package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ConsistencyTracker<Solution_> {
    private final IdentityHashMap<Object, Boolean> entityToIsInconsistentMap = new IdentityHashMap<>();
    private final HashMap<Class<?>, EntityConsistencyState<Solution_>> entityClassToConsistencyStateMap = new HashMap<>();

    public EntityConsistencyState<Solution_>
            getDeclarativeEntityConsistencyState(EntityDescriptor<Solution_> entityDescriptor) {
        return entityClassToConsistencyStateMap.computeIfAbsent(entityDescriptor.getEntityClass(),
                ignored -> new EntityConsistencyState<>(entityDescriptor, entityToIsInconsistentMap));
    }

    public void initializeForEntities(SolutionDescriptor<Solution_> solutionDescriptor,
            Object... entityOrFacts) {
        var entityList = new ArrayList<>(entityOrFacts.length);
        for (Object entityOrFact : entityOrFacts) {
            if (solutionDescriptor.hasEntityDescriptor(entityOrFact.getClass())) {
                entityList.add(entityOrFact);
            }
        }
        var graph = DefaultShadowVariableSessionFactory.buildGraphForStructureAndDirection(
                // We want an ARBITRARY graph structure, since other structures can preemptively
                // set inconsistency of entities, which ConstraintVerifier does not want
                // if the user specified a specific inconsistency value.
                new GraphStructure.GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null),
                this, solutionDescriptor,
                new VariableReferenceGraphBuilder<>(ChangedVariableNotifier.empty()),
                entityList.toArray(),
                DefaultTopologicalOrderGraph::new);
        graph.setUnknownInconsistencyValues();
    }
}
