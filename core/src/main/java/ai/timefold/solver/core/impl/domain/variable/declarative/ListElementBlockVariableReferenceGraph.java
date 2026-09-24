package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import ai.timefold.solver.core.api.score.analysis.EntityVariablePair;
import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A variable reference graph that excludes a planning list variable's elements from the graph
 * and represents each list entity's chain by a single block node instead;
 * see {@link GraphStructure#LIST_ELEMENT_BLOCK}.
 * <p>
 * The graph itself covers everything else and is built by the normal machinery,
 * with fixed edges from each entity's pre-chain variables to its block node
 * and from its block node to its post-chain variables;
 * a single {@link #updateChanged()} pass in topological order therefore walks each dirty chain
 * exactly once, after its pre-chain variables and before its post-chain variables.
 * This wrapper only routes the events the block nodes need:
 * it records the elements whose source variables changed and the list entities whose list changed,
 * classifies the elements into seeds of their entities' chains,
 * and marks the dirty entities' block nodes before delegating the update.
 * It also marks the list entity's post-chain variables changed on a list change,
 * which the graph derives from the list element locators the block node skips.
 * <p>
 * The classification runs before the delegated update, and recomputes the elements that left their
 * list along the way; those writes come back here through the score director. The graph's own
 * reentrancy guard does not cover that window, since it only spans the update it delegates to,
 * hence {@link #isUpdating}: without it the classification would add to the list it is iterating.
 */
@NullMarked
final class ListElementBlockVariableReferenceGraph<Solution_> implements VariableReferenceGraph {

    // These are immutable.
    private final VariableReferenceGraph innerGraph;
    private final @Nullable AbstractVariableReferenceGraph<Solution_, ?> innerNodeGraph;
    private final ListElementBlockUpdater<Solution_> blockUpdater;
    private final String listVariableName;
    private final Class<?> elementEntityClass;
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    /**
     * Owner to block node. Hoisted at construction because the per-variable map is keyed by
     * {@link VariableMetaModel}, whose equals is expensive, and this lookup runs for every dirty chain.
     */
    private final Map<Object, GraphNode<Solution_>> ownerToBlockNodeMap;
    /**
     * The list variable's after processors, which mark the list entity's post-chain variables changed.
     * Hoisted at construction for the same reason as {@link #ownerToBlockNodeMap}.
     */
    private final List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> listVariableAfterProcessorList;

    // This is mutable.
    private boolean isUpdating;

    ListElementBlockVariableReferenceGraph(
            VariableReferenceGraph innerGraph,
            ListElementBlockUpdater<Solution_> blockUpdater,
            VariableMetaModel<Solution_, ?, ?> listVariableMetaModel,
            Class<?> elementEntityClass,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> elementDescriptorList,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        this.innerGraph = innerGraph;
        // A graph without nodes, hence without block nodes, only comes out of a solution that has no
        // list entity at all. Every element is then unassigned, and classifyChangedElements computes
        // those directly, so there is nothing left for a block node to do.
        this.innerNodeGraph = innerGraph instanceof AbstractVariableReferenceGraph<?, ?> abstractGraph
                ? (AbstractVariableReferenceGraph<Solution_, ?>) abstractGraph
                : null;
        this.blockUpdater = blockUpdater;
        this.listVariableName = listVariableMetaModel.name();
        this.elementEntityClass = elementEntityClass;
        this.changedVariableNotifier = changedVariableNotifier;
        this.ownerToBlockNodeMap = innerNodeGraph == null ? Map.of()
                : innerNodeGraph.variableReferenceToContainingNodeMap.getOrDefault(listVariableMetaModel, Map.of());
        this.listVariableAfterProcessorList = innerNodeGraph == null ? List.of()
                : innerNodeGraph.variableReferenceToAfterProcessor.getOrDefault(listVariableMetaModel, List.of());
        this.isUpdating = false;

        this.monitoredSourceVariableSet = new HashSet<>();
        for (var descriptor : elementDescriptorList) {
            for (var source : descriptor.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    monitoredSourceVariableSet.add(sourceReference.variableMetaModel());
                }
            }
        }

        // Every element gets an initial computation and starts consistent;
        // its consistency can only change when its entity becomes inconsistent.
        for (var entity : entities) {
            if (elementEntityClass.isInstance(entity)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, entity, false);
                blockUpdater.recordChangedElement(entity);
            }
        }
        updateChanged();
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (isUpdating) {
            // A reentrant event of this graph's own update.
            return;
        }
        innerGraph.beforeVariableChanged(variableReference, entity);
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (isUpdating) {
            return;
        }
        if (monitoredSourceVariableSet.contains(variableReference) && elementEntityClass.isInstance(entity)) {
            blockUpdater.recordChangedElement(entity);
        }
        innerGraph.afterVariableChanged(variableReference, entity);
    }

    @Override
    public void beforeListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Nothing is recorded here: the list variable state changes the inverse and the previous or next
        // element of the elements that leave the list, and afterVariableChanged records them from those.
        innerGraph.beforeListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Delegated first, so that the graph fails fast on a list change during an update
        // before anything is recorded.
        innerGraph.afterListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            blockUpdater.recordChangedList(entity);
        }
        markPostChainVariablesChanged(entity);
    }

    @Override
    public boolean updateChanged() {
        isUpdating = true;
        var isUpdated = false;
        try {
            blockUpdater.classifyChangedElements(changedVariableNotifier, this::markBlockNodeChanged);
            isUpdated = innerGraph.updateChanged();
            return isUpdated;
        } finally {
            blockUpdater.endUpdate(isUpdated);
            isUpdating = false;
        }
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        var innerVariableLoopList = innerGraph.getVariableLoops();
        var variableLoopList = new ArrayList<VariableLoop>(innerVariableLoopList.size());
        for (var innerVariableLoop : innerVariableLoopList) {
            // A loop that closes through a chain runs through its entity's block node, which the graph
            // reports as the list variable; like the arbitrary graph, report the chain's elements instead.
            var involvedVariableSet = new LinkedHashSet<EntityVariablePair>();
            for (var entityVariablePair : innerVariableLoop.involvedVariableSet()) {
                var entity = entityVariablePair.entity();
                if (entityVariablePair.variableName().equals(listVariableName) && ownerToBlockNodeMap.containsKey(entity)) {
                    blockUpdater.addElementVariables(entity, involvedVariableSet);
                } else {
                    involvedVariableSet.add(entityVariablePair);
                }
            }
            variableLoopList.add(new VariableLoop(involvedVariableSet));
        }
        return variableLoopList;
    }

    /**
     * Stands in for the mark the graph derives from the list element locators, which the block node
     * skips along with the edges. Without it, removing the list's last element would leave no element
     * to walk and no edge to the entity, so nothing would recompute its post-chain variables.
     */
    private void markPostChainVariablesChanged(Object entity) {
        var nodeGraph = innerNodeGraph;
        if (nodeGraph == null) {
            return;
        }
        nodeGraph.processEntity(listVariableAfterProcessorList, entity);
    }

    private void markBlockNodeChanged(Object owner) {
        var nodeGraph = innerNodeGraph;
        if (nodeGraph == null) {
            return;
        }
        // Every list entity of the solution the graph was built for has a block node.
        nodeGraph.markChanged(Objects.requireNonNull(ownerToBlockNodeMap.get(owner)));
    }
}
