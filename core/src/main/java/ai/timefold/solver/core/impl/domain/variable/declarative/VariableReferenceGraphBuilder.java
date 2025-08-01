package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NonNull;

public final class VariableReferenceGraphBuilder<Solution_> {

    final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToBeforeProcessor;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>>> variableReferenceToAfterProcessor;
    final List<EntityVariablePair<Solution_>> instanceList;
    final Map<EntityVariablePair<Solution_>, List<EntityVariablePair<Solution_>>> fixedEdges;
    final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;
    final Map<Integer, Map<Object, EntityVariablePair<Solution_>>> variableGroupIdToInstanceMap;
    boolean isGraphFixed;

    public VariableReferenceGraphBuilder(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        instanceList = new ArrayList<>();
        variableReferenceToInstanceMap = new HashMap<>();
        variableGroupIdToInstanceMap = new HashMap<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
        isGraphFixed = true;
    }

    public <Entity_> void addVariableReferenceEntity(Entity_ entity, List<VariableUpdaterInfo<Solution_>> variableReferences) {
        var groupId = variableReferences.get(0).groupId();
        var isGroup = variableReferences.get(0).groupEntities() != null;
        var entityRepresentative = entity;
        if (isGroup) {
            entityRepresentative = (Entity_) variableReferences.get(0).groupEntities()[0];
        }
        var instanceMap = variableGroupIdToInstanceMap.get(groupId);

        var instance = instanceMap == null ? null : instanceMap.get(entityRepresentative);
        if (instance != null) {
            return;
        }
        if (instanceMap == null) {
            instanceMap = new IdentityHashMap<>();
            variableGroupIdToInstanceMap.put(groupId, instanceMap);
        }
        var node = new EntityVariablePair<>(entityRepresentative, variableReferences, instanceList.size());

        if (isGroup) {
            for (var groupEntity : variableReferences.get(0).groupEntities()) {
                addToInstanceMaps(instanceMap, groupEntity, node, variableReferences);
            }
        } else {
            addToInstanceMaps(instanceMap, entity, node, variableReferences);
        }
        instanceList.add(node);
    }

    private void addToInstanceMaps(Map<Object, EntityVariablePair<Solution_>> instanceMap,
            Object entity, EntityVariablePair<Solution_> node, List<VariableUpdaterInfo<Solution_>> variableReferences) {
        instanceMap.put(entity, node);
        for (var variable : variableReferences) {
            var variableInstanceMap =
                    variableReferenceToInstanceMap.computeIfAbsent(variable.id(), ignored -> new IdentityHashMap<>());
            variableInstanceMap.put(entity, node);
        }
    }

    public void addFixedEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
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
        if (instanceList.isEmpty()) {
            return EmptyVariableReferenceGraph.INSTANCE;
        }
        if (isGraphFixed) {
            return new FixedVariableReferenceGraph<>(this, graphCreator);
        }
        return new DefaultVariableReferenceGraph<>(this, graphCreator);
    }

    public @NonNull EntityVariablePair<Solution_> lookupOrError(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var out = variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
        if (out == null) {
            throw new IllegalArgumentException();
        }
        return out;
    }

}
