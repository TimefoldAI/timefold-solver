package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ConsistencyTracker<Solution_>(
        IdentityHashMap<Object, Boolean> entityToIsInconsistentMap,
        HashMap<Class<?>, EntityConsistencyState<Solution_, Object>> entityClassToConsistencyStateMap,
        boolean isFrozen) {
    public ConsistencyTracker() {
        this(new IdentityHashMap<>(), new HashMap<>(), false);
    }

    private ConsistencyTracker(boolean isFrozen) {
        this(new IdentityHashMap<>(), new HashMap<>(), isFrozen);
    }

    public static <Solution_> ConsistencyTracker<Solution_> frozen(SolutionDescriptor<Solution_> solutionDescriptor,
            Object[] entityOrFacts) {
        var out = new ConsistencyTracker<Solution_>(true);
        out.setUnknownConsistencyFromEntityShadowVariablesInconsistent(solutionDescriptor, entityOrFacts);
        return out;
    }

    public EntityConsistencyState<Solution_, Object>
            getDeclarativeEntityConsistencyState(EntityDescriptor<Solution_> entityDescriptor) {
        return entityClassToConsistencyStateMap.computeIfAbsent(entityDescriptor.getEntityClass(),
                ignored -> new EntityConsistencyState<>(entityDescriptor, entityToIsInconsistentMap));
    }

    /**
     * Used by ConstraintVerifier to set consistency of entities that either:
     * <ul>
     * <li>Do not have a {@link ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent} member.</li>
     * <li>Have a {@link ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent} member that is null.</li>
     * </ul>
     * If an entity has a {@link ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent} member that is
     * either true or false, then that value determines if the entity is consistent or not
     * (regardless of its actual consistency in the graph).
     */
    void setUnknownConsistencyFromEntityShadowVariablesInconsistent(SolutionDescriptor<Solution_> solutionDescriptor,
            Object[] entityOrFacts) { // Not private so DefaultVariableReferenceGraph javadoc can reference it.
        var entities = Arrays.stream(entityOrFacts)
                .filter(maybeEntity -> solutionDescriptor.hasEntityDescriptor(maybeEntity.getClass()))
                .toArray(Object[]::new);

        var graph = DefaultShadowVariableSessionFactory
                .buildGraphForStructureAndDirection(
                        // We want an ARBITRARY graph structure, since other structures can preemptively
                        // set inconsistency of entities, which ConstraintVerifier does not want
                        // if the user specified a specific inconsistency value.
                        new GraphStructure.GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null),
                        new DefaultShadowVariableSessionFactory.GraphDescriptor<>(solutionDescriptor,
                                ChangedVariableNotifier.empty(),
                                entities).withConsistencyTracker(this).withDiscoveredReferencedEntities());

        // Graph will either be DefaultVariableReferenceGraph or EmptyVariableReferenceGraph
        // If it is empty, we don't need to do anything.
        if (graph instanceof DefaultVariableReferenceGraph<?> defaultVariableReferenceGraph) {
            defaultVariableReferenceGraph.setUnknownInconsistencyValues();
        }
    }

    /**
     * If true, consistency and shadow variables are frozen and should not be updated.
     * ConstraintVerifier creates a frozen instance via {@link #frozen(SolutionDescriptor, Object[])}.
     * 
     * @return true if consistency and shadow variables are frozen and should not be updated
     */
    public boolean isFrozen() {
        return isFrozen;
    }
}
