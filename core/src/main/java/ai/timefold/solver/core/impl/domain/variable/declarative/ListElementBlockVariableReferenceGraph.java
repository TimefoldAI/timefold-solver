package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

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
 * it records the elements whose source variables changed and the list variables' change ranges,
 * classifies them into per-entity dirty ranges,
 * and marks the dirty entities' block nodes before delegating the update.
 * It also marks the list entity's post-chain variables changed on a list change,
 * which the graph derives from the list element locators the block node skips.
 * <p>
 * The classification runs before the delegated update, and recomputes the elements that left their
 * list along the way; those writes come back here through the score director. The graph's own
 * reentrancy guard does not cover that window, since it only spans the update it delegates to,
 * hence {@link #isProcessing}: without it the classification would add to the list it is iterating.
 */
@NullMarked
final class ListElementBlockVariableReferenceGraph<Solution_> implements VariableReferenceGraph {

    private final VariableReferenceGraph innerGraph;
    private final @Nullable AbstractVariableReferenceGraph<Solution_, ?> innerNodeGraph;
    private final ListElementBlockUpdater<Solution_> blockUpdater;
    private final Class<?> elementEntityClass;
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    /**
     * Owner to block node. Hoisted at construction because the per-variable map is keyed by
     * {@link VariableMetaModel}, whose equals is expensive, and this lookup runs per changed element.
     */
    private final Map<Object, GraphNode<Solution_>> ownerToBlockNodeMap;
    /**
     * The list variable's after processors, which mark the list entity's post-chain variables changed.
     * Hoisted at construction for the same reason as {@link #ownerToBlockNodeMap}.
     */
    private final List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> listVariableAfterProcessorList;

    private boolean isProcessing;

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
        this.elementEntityClass = elementEntityClass;
        this.changedVariableNotifier = changedVariableNotifier;
        this.ownerToBlockNodeMap = innerNodeGraph == null ? Map.of()
                : innerNodeGraph.variableReferenceToContainingNodeMap.getOrDefault(listVariableMetaModel, Map.of());
        this.listVariableAfterProcessorList = innerNodeGraph == null ? List.of()
                : innerNodeGraph.variableReferenceToAfterProcessor.getOrDefault(listVariableMetaModel, List.of());
        this.isProcessing = false;

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
        if (isProcessing) {
            // A reentrant event of this graph's own update.
            return;
        }
        innerGraph.beforeVariableChanged(variableReference, entity);
    }

    @Override
    public void afterVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (isProcessing) {
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
        // The range is only recorded on the after event: an element that leaves the list keeps no
        // trace of it there, but the list variable state supply changes its inverse and its previous
        // or next element, and afterVariableChanged records it from those.
        // ListElementBlockShadowVariableTest#removingTheLastElementOfARouteUpdatesItsEntity pins it.
        innerGraph.beforeListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        // Delegated first, so that the graph fails fast on a list change during an update
        // before anything is recorded.
        innerGraph.afterListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
        if (fromIndex < toIndex) {
            // The changed elements are classified into a dirty range, and every element between
            // these two ends up inside it, whichever way round the chain order runs.
            blockUpdater.recordChangedElement(elementList.get(fromIndex));
            blockUpdater.recordChangedElement(elementList.get(toIndex - 1));
        }
        markPostChainVariablesChanged(entity);
    }

    @Override
    public boolean updateChanged() {
        isProcessing = true;
        try {
            blockUpdater.classifyChangedElements(changedVariableNotifier, this::markBlockNodeChanged);
            var success = innerGraph.updateChanged();
            // A flag whose block node was never processed must not leak into the next update, whether
            // because its entity left the working solution or because the graph gave up on a
            // structurally flawed solution. Nothing is lost by dropping it: every flag comes from the
            // events of the change being processed, and the caller undoes that change and updates
            // again, which raises the same events the other way around.
            blockUpdater.clearTransientState();
            return success;
        } finally {
            isProcessing = false;
        }
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        // A loop that closes through a chain runs through the entity's block node,
        // which the graph reports as its list variable; its elements follow their entity.
        return innerGraph.getVariableLoops();
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
        var blockNode = ownerToBlockNodeMap.get(owner);
        if (blockNode == null) {
            throw new IllegalStateException(
                    "Impossible state: the list entity (%s) has no block node in the graph.".formatted(owner));
        }
        nodeGraph.markChanged(blockNode);
    }
}
