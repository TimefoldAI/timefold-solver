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

import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

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

    public BaseTopologicalOrderGraph.NodeTopologicalOrder[] buildNodeTopologicalOrderArray(BaseTopologicalOrderGraph graph) {
        var out = new BaseTopologicalOrderGraph.NodeTopologicalOrder[nodeList.size()];

        for (var i = 0; i < out.length; i++) {
            out[i] = new BaseTopologicalOrderGraph.NodeTopologicalOrder(i, graph);
        }
        return out;
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

        if (changedBitSet.cardinality() == 0) {
            // No node's loop status changed, so the graph does
            // not have any fixed loops.
            return;
        }

        // At least one node's loop status has changed,
        // and since the empty graph has no loops, that
        // mean there is at least one fixed loop in the graph.
        var loopedComponents = graph.getLoopedComponentList();
        var limit = 3;
        var isLimited = loopedComponents.size() > limit;
        var loopedVariables = new LinkedHashSet<VariableMetaModel<?, ?, ?>>();
        var nodeCycleList = loopedComponents.stream()
                .map(nodeIds -> nodeIds.stream().mapToInt(Integer::intValue).mapToObj(nodeList::get).toList())
                .toList();

        for (var cycle : nodeCycleList) {
            cycle.stream().flatMap(node -> node.variableReferences().stream())
                    .map(VariableUpdaterInfo::id)
                    .forEach(loopedVariables::add);
        }

        var out = new StringBuilder("There are fixed dependency loops in the graph for variables %s:%n"
                .formatted(loopedVariables));

        for (var cycle : nodeCycleList) {
            out.append(cycle.stream()
                    .map(GraphNode::toString)
                    .collect(Collectors.joining(", ",
                            "- [",
                            "] ")));
        }

        if (isLimited) {
            out.append("- ...(");
            out.append(loopedComponents.size() - limit);
            out.append(" more)%n");
        }
        out.append(
                """

                        Fixed dependency loops indicate a problem in either the input problem or in the @%s of the looped @%s.
                        There are two kinds of fixed dependency loops:

                        - You have two shadow variables whose sources refer to each other;
                          this is called a source-induced fixed loop.
                          In code, this situation looks like this:

                              @ShadowVariable(supplierName="variable1Supplier")
                              String variable1;

                              @ShadowVariable(supplierName="variable2Supplier")
                              String variable2;

                              // ...

                              @ShadowSources("variable2")
                              String variable1Supplier() { /* ... */ }

                              @ShadowSources("variable1")
                              String variable2Supplier() { /* ... */ }

                        - You have a shadow variable whose sources refer to itself transitively via a fact;
                          this is called a fact-induced fixed loop.
                          In code, this situation looks like this:

                              @PlanningEntity
                              public class Entity {
                                  Entity dependency;

                                  @ShadowVariable(supplierName="variableSupplier")
                                  String variable;

                                  @ShadowSources("dependency.variable")
                                  String variableSupplier() { /* ... */ }
                                  // ...
                              }

                              Entity a = new Entity();
                              Entity b = new Entity();
                              a.setDependency(b);
                              b.setDependency(a);
                              // a depends on b, and b depends on a, which is invalid.


                        The solver cannot break a fixed loop since the loop is caused by sources or facts instead of variables.
                        Fixed loops should not be confused with variable-induced loops, which can be broken by the solver:

                              @PlanningEntity
                              public class Entity {
                                  Entity dependency;

                                  @PreviousElementShadowVariable(/* ... */)
                                  Entity previous;

                                  @ShadowVariable(supplierName="variableSupplier")
                                  String variable;

                                  @ShadowSources({"previous.variable", "dependency.variable"})
                                  String variable1Supplier() { /* ... */ }
                                  // ...
                              }

                              Entity a = new Entity();
                              Entity b = new Entity();
                              b.setDependency(a);
                              a.setPrevious(b);
                              // b depends on a via a fact, and a depends on b via a variable
                              // The solver can break this loop by moving a after b.


                        Maybe check none of your @%s form a loop on the same entity.
                        """
                        .formatted(ShadowSources.class.getSimpleName(), ShadowVariable.class.getSimpleName(),
                                ShadowSources.class.getSimpleName()));
        throw new IllegalArgumentException(out.toString());
    }

}
