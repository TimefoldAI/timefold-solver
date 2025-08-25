package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

import org.jspecify.annotations.NonNull;

public final class VariableReferenceGraphBuilder<Solution_> {

    final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToBeforeProcessor;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToAfterProcessor;
    final List<GraphNode<Solution_>> nodeList;
    final Map<Object, Integer> entityToEntityId;
    final Map<GraphNode<Solution_>, List<GraphNode<Solution_>>> fixedEdges;
    final Map<VariableMetaModel<?, ?, ?>, Map<Object, GraphNode<Solution_>>> variableReferenceToContainingNodeMap;
    final Map<Integer, Map<Object, GraphNode<Solution_>>> variableGroupIdToContainingNodeMap;
    boolean isGraphFixed;

    public VariableReferenceGraphBuilder(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        nodeList = new ArrayList<>();
        variableReferenceToContainingNodeMap = new HashMap<>();
        variableGroupIdToContainingNodeMap = new HashMap<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
        entityToEntityId = new IdentityHashMap<>();
        isGraphFixed = true;
    }

    public <Entity_> void addVariableReferenceEntity(Entity_ entity, List<VariableUpdaterInfo<Solution_>> variableReferences) {
        var groupId = variableReferences.get(0).groupId();
        var isGroup = variableReferences.get(0).groupEntities() != null;
        var entityRepresentative = entity;
        if (isGroup) {
            entityRepresentative = (Entity_) variableReferences.get(0).groupEntities()[0];
        }
        var instanceMap = variableGroupIdToContainingNodeMap.get(groupId);

        var instance = instanceMap == null ? null : instanceMap.get(entityRepresentative);
        if (instance != null) {
            return;
        }
        if (instanceMap == null) {
            instanceMap = new IdentityHashMap<>();
            variableGroupIdToContainingNodeMap.put(groupId, instanceMap);
        }

        var entityId = entityToEntityId.computeIfAbsent(entityRepresentative, ignored -> entityToEntityId.size());
        int[] groupEntityIds = null;

        if (isGroup) {
            var groupEntities = variableReferences.get(0).groupEntities();
            groupEntityIds = new int[groupEntities.length];
            for (var i = 0; i < groupEntityIds.length; i++) {
                var groupEntity = variableReferences.get(0).groupEntities()[i];
                groupEntityIds[i] = entityToEntityId.computeIfAbsent(groupEntity, ignored -> entityToEntityId.size());
            }
        }

        var node = new GraphNode<>(entityRepresentative, variableReferences, nodeList.size(),
                entityId, groupEntityIds);

        if (isGroup) {
            for (var groupEntity : variableReferences.get(0).groupEntities()) {
                addToInstanceMaps(instanceMap, groupEntity, node, variableReferences);
            }
        } else {
            addToInstanceMaps(instanceMap, entity, node, variableReferences);
        }
        nodeList.add(node);
    }

    private void addToInstanceMaps(Map<Object, GraphNode<Solution_>> instanceMap,
            Object entity, GraphNode<Solution_> node, List<VariableUpdaterInfo<Solution_>> variableReferences) {
        instanceMap.put(entity, node);
        for (var variable : variableReferences) {
            var variableInstanceMap =
                    variableReferenceToContainingNodeMap.computeIfAbsent(variable.id(), ignored -> new IdentityHashMap<>());
            variableInstanceMap.put(entity, node);
        }
    }

    public void addFixedEdge(@NonNull GraphNode<Solution_> from, @NonNull GraphNode<Solution_> to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addBeforeProcessor(GraphChangeType graphChangeType, VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object> consumer) {
        isGraphFixed &= !graphChangeType.affectsGraph();
        variableReferenceToBeforeProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void addAfterProcessor(GraphChangeType graphChangeType, VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object> consumer) {
        isGraphFixed &= !graphChangeType.affectsGraph();
        variableReferenceToAfterProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public VariableReferenceGraph build(IntFunction<TopologicalOrderGraph> graphCreator) {
        assertNoFixedLoops();
        if (nodeList.isEmpty()) {
            return EmptyVariableReferenceGraph.INSTANCE;
        }
        if (isGraphFixed) {
            return new FixedVariableReferenceGraph<>(this, graphCreator);
        }
        return new DefaultVariableReferenceGraph<>(this, graphCreator);
    }

    public @NonNull GraphNode<Solution_> lookupOrError(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var out = variableReferenceToContainingNodeMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
        if (out == null) {
            throw new IllegalArgumentException();
        }
        return out;
    }

    private void assertNoFixedLoops() {
        var graph = new DefaultTopologicalOrderGraph(nodeList.size());
        for (var fixedEdge : fixedEdges.entrySet()) {
            var fromNodeId = fixedEdge.getKey().graphNodeId();
            for (var toNode : fixedEdge.getValue()) {
                var toNodeId = toNode.graphNodeId();
                graph.addEdge(fromNodeId, toNodeId);
            }
        }

        var changedBitSet = new BitSet();
        graph.commitChanges(changedBitSet);

        if (changedBitSet.cardinality() != 0) {
            // At least one node's loop status has changed,
            // and since the empty graph has no loops, that
            // mean there is at least one fixed loop in the graph.
            var loopedComponents = graph.getLoopedComponentList();
            var limit = 3;
            var isLimited = loopedComponents.size() > limit;
            var loopedVariables = new LinkedHashSet<VariableMetaModel<?, ?, ?>>();

            for (var loopedComponent : loopedComponents) {
                loopedComponent.stream()
                        .map(nodeList::get)
                        .forEach(node -> {
                            node.variableReferences().stream()
                                    .map(VariableUpdaterInfo::id)
                                    .forEach(loopedVariables::add);
                        });
            }

            var out = new StringBuilder("There are fixed dependency loops in the graph for variables %s:\n"
                    .formatted(loopedVariables));
            for (var i = 0; i < Math.min(loopedComponents.size(), limit); i++) {
                out.append(
                        loopedComponents.get(i)
                                .stream()
                                .map(nodeList::get)
                                .map(GraphNode::toString)
                                .collect(Collectors.joining(", ",
                                        "- [",
                                        "]\n")));
            }
            if (isLimited) {
                out.append("- ...(");
                out.append(loopedComponents.size() - limit);
                out.append(" more)\n");
            }
            out.append("""

                    Fixed dependency loops indicate a problem in either the input problem or in the @%s of the looped @%s.
                    If a downstream variable depends on an upstream variable, the upstream variable cannot depend on the
                    downstream variable on the same entity. As such, a model that looks like this is invalid:

                    entity1:variable1 (source)-> entity1:variable2 (source)-> entity1:variable1
                    In code, this situation looks like this:

                        @ShadowSources("variable2")
                        String variable1Supplier() { /* ... */ }

                        @ShadowSources("variable1")
                        String variable2Supplier() { /* ... */ }

                    This applies even if the upstream variable is reached indirectly:

                    entity1:variable1 (fact)-> entity2:variable1 (fact)-> entity1:variable1
                    In code, this situation looks like this:

                        @PlanningEntity
                        public class Entity {
                            Entity fact;

                            @ShadowSources("fact.variable1")
                            String variable1Supplier() { /* ... */ }
                            // ...
                        }

                        Entity a = new Entity();
                        Entity b = new Entity();
                        a.setFact(b);
                        b.setFact(a);
                        // a depends on b, and b depends on a, which is invalid.

                    Maybe check none of your @%s form a loop on the same entity.
                    """.formatted(ShadowSources.class.getSimpleName(), ShadowVariable.class.getSimpleName(),
                    ShadowSources.class.getSimpleName()));
            throw new IllegalArgumentException(out.toString());
        }
    }

}
