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
import org.jspecify.annotations.Nullable;

public class VariableReferenceGraph<Solution_> {

    final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<WorkingReferenceGraph<Solution_>, Object>>> variableReferenceToBeforeProcessor;
    final Map<VariableMetaModel<?, ?, ?>, List<BiConsumer<WorkingReferenceGraph<Solution_>, Object>>> variableReferenceToAfterProcessor;
    final List<EntityVariablePair<Solution_>> instanceList;
    final Map<EntityVariablePair<Solution_>, List<EntityVariablePair<Solution_>>> fixedEdges;
    final Map<VariableMetaModel<?, ?, ?>, Map<Object, EntityVariablePair<Solution_>>> variableReferenceToInstanceMap;

    /**
     * Once first created during {@link #initialize(IntFunction)},
     * this entire class effectively becomes immutable.
     * Attempts to mutate will throw exceptions.
     * This is useful to establish invariants
     * on what is allowed to change when and from where.
     */
    private WorkingReferenceGraph<Solution_> workingReferenceGraph;

    public VariableReferenceGraph(ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        this.changedVariableNotifier = changedVariableNotifier;
        instanceList = new ArrayList<>();
        variableReferenceToInstanceMap = new HashMap<>();
        variableReferenceToBeforeProcessor = new HashMap<>();
        variableReferenceToAfterProcessor = new HashMap<>();
        fixedEdges = new HashMap<>();
    }

    public <Entity_> void addVariableReferenceEntity(Entity_ entity, VariableUpdaterInfo<Solution_> variableReference) {
        if (workingReferenceGraph != null) {
            throw new IllegalStateException("The graph has already been initialized.");
        }
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
        if (workingReferenceGraph != null) {
            throw new IllegalStateException("The graph has already been initialized.");
        }
        if (from.graphNodeId() == to.graphNodeId()) {
            return;
        }
        fixedEdges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    }

    public void addBeforeProcessor(VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<WorkingReferenceGraph<Solution_>, Object> consumer) {
        if (workingReferenceGraph != null) {
            throw new IllegalStateException("The graph has already been initialized.");
        }
        variableReferenceToBeforeProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    public void addAfterProcessor(VariableMetaModel<?, ?, ?> variableId,
            BiConsumer<WorkingReferenceGraph<Solution_>, Object> consumer) {
        if (workingReferenceGraph != null) {
            throw new IllegalStateException("The graph has already been initialized.");
        }
        variableReferenceToAfterProcessor.computeIfAbsent(variableId, k -> new ArrayList<>())
                .add(consumer);
    }

    @SuppressWarnings("unchecked")
    public void initialize(IntFunction<TopologicalOrderGraph> graphCreator) {
        // TODO empty shows up in VRP example when using it as CVRP, not CVRPTW
        //  In that case, TimeWindowedCustomer does not exist
        //  and therefore Customer has no shadow variable.
        //  Surely there has to be an earlier way to catch this?
        this.workingReferenceGraph = instanceList.isEmpty() ? EmptyWorkingReferenceGraph.INSTANCE
                : new DefaultWorkingReferenceGraph<>(this, graphCreator);
    }

    public @Nullable EntityVariablePair<Solution_> lookupOrNull(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        if (workingReferenceGraph == null) {
            return variableReferenceToInstanceMap.getOrDefault(variableId, Collections.emptyMap()).get(entity);
        } else {
            return workingReferenceGraph.lookupOrNull(variableId, entity);
        }
    }

    public @NonNull EntityVariablePair<Solution_> lookupOrError(VariableMetaModel<?, ?, ?> variableId, Object entity) {
        var out = lookupOrNull(variableId, entity);
        if (out == null) {
            throw new IllegalArgumentException();
        }
        return out;
    }

    public void updateChanged() {
        if (workingReferenceGraph == null) {
            throw new IllegalStateException("The graph has not been initialized yet.");
        }
        workingReferenceGraph.updateChanged();
    }

    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
            processEntity(workingReferenceGraph,
                    variableReferenceToBeforeProcessor.getOrDefault(variableReference, Collections.emptyList()), entity);
        }
    }

    private static <Solution_> void processEntity(WorkingReferenceGraph<Solution_> workingReferenceGraph,
            List<BiConsumer<WorkingReferenceGraph<Solution_>, Object>> processorList, Object entity) {
        var processorCount = processorList.size();
        // Avoid creation of iterators on the hot path.
        // The short-lived instances were observed to cause considerable GC pressure.
        for (int i = 0; i < processorCount; i++) {
            processorList.get(i).accept(workingReferenceGraph, entity);
        }
    }

    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        afterVariableChanged(workingReferenceGraph, variableReference, entity);
    }

    // Will be called by the WorkingReferenceGraph while it is being created.
    // Therefore we need to supply the instance externally and not from the field.
    void afterVariableChanged(WorkingReferenceGraph<Solution_> workingReferenceGraph,
            VariableMetaModel<?, ?, ?> variableReference,
            Object entity) {
        if (variableReference.entity().type().isInstance(entity)) {
            var node = workingReferenceGraph.lookupOrNull(variableReference, entity);
            if (node != null) {
                workingReferenceGraph.markChanged(node);
            }
            processEntity(workingReferenceGraph,
                    variableReferenceToAfterProcessor.getOrDefault(variableReference, Collections.emptyList()), entity);
        }
    }

    @Override
    public String toString() {
        return "VariableReferenceGraph{%s}".formatted(workingReferenceGraph);
    }

}
