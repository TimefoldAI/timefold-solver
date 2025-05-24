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
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToBeforeProcessor;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<VariableReferenceGraph<Solution_>, Object>>> variableReferenceToAfterProcessor;
    final List<EntityVariablePair<Solution_>> instanceList;
    final Map<EntityVariablePair<Solution_>, List<EntityVariablePair<Solution_>>> fixedEdges;
    final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;

    public VariableReferenceGraphBuilder(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        instanceList = new ArrayList<>();
        variableReferenceToInstanceMap = new HashMap<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
    }

    public <Entity_> void addVariableReferenceEntity(Entity_ entity, VariableUpdaterInfo<Solution_> variableReference) {
        var variableId = variableReference.id();
        var instanceMap = variableReferenceToInstanceMap.get(variableId);
        var instance = instanceMap == null ? null : instanceMap.get(entity);
        if (instance != null) {
            return;
        }
        if (instanceMap == null) {
            instanceMap = new IdentityHashMap<>();
            variableReferenceToInstanceMap.put(variableId, instanceMap);
        }
        var node = new EntityVariablePair<>(entity, variableReference, instanceList.size());
        instanceMap.put(entity, node);
        instanceList.add(node);
    }

    public void addFixedEdge(@NonNull EntityVariablePair<Solution_> from, @NonNull EntityVariablePair<Solution_> to) {
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addBeforeProcessor(VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<VariableReferenceGraph<Solution_>, Object> consumer) {
        variableReferenceToBeforeProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void addAfterProcessor(VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<VariableReferenceGraph<Solution_>, Object> consumer) {
        variableReferenceToAfterProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    @SuppressWarnings("unchecked")
    public VariableReferenceGraph<Solution_> build(IntFunction<TopologicalOrderGraph> graphCreator) {
        // TODO empty shows up in VRP example when using it as CVRP, not CVRPTW
        //  In that case, TimeWindowedCustomer does not exist
        //  and therefore Customer has no shadow variable.
        //  Surely there has to be an earlier way to catch this?
        return instanceList.isEmpty() ? EmptyVariableReferenceGraph.INSTANCE
                : new DefaultVariableReferenceGraph<>(this, graphCreator);
    }

    public @NonNull EntityVariablePair<Solution_> lookupOrError(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var out = variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
        if (out == null) {
            throw new IllegalArgumentException();
        }
        return out;
    }

}
