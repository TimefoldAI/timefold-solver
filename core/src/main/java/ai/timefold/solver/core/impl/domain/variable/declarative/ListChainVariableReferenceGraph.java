package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.domain.variable.declarative.VariableReferenceGraph.VariableChangeHook;
import ai.timefold.solver.core.impl.util.ShrinkingIdentityHashMap;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A variable reference graph that excludes a planning list variable's elements from the graph
 * and represents each list entity's chain by a single node instead;
 * see {@link GraphStructure#LIST_CHAIN}.
 * <p>
 * The wrapped graph covers everything else, with fixed edges from each list entity's pre-chain variables
 * to its chain node and from its chain node to its post-chain variables,
 * so one {@link #updateChanged()} pass walks each dirty chain once, in between.
 * This wrapper records the elements whose source variables changed and the list entities whose list changed,
 * and marks their chain nodes changed before delegating the update.
 */
@NullMarked
final class ListChainVariableReferenceGraph<Solution_> implements VariableReferenceGraph {

    // These are immutable.
    private final AbstractVariableReferenceGraph<Solution_, ?> innerGraph;
    private final ListChainUpdater<Solution_> chainUpdater;
    private final ListVariableState<Solution_, Object, Object> listVariableState;
    private final Class<?> elementEntityClass;
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;

    // These are mutable.
    // Resolved once per variable, as the graph it wraps does.
    private final Map<VariableMetaModel<?, ?, ?>, @Nullable VariableChangeHook> variableReferenceToHookMap;
    private final List<Object> changedElementList;
    // The unassigned elements the classification in progress recomputed, so that each is recomputed once.
    // Its clear() keeps the update cheap even after the initial update, which records every unassigned element.
    private final ShrinkingIdentityHashMap<Object, Boolean> recomputedUnassignedElementMap;
    private final List<ListChainUpdater.ChainState<Solution_>> dirtyChainStateList;
    // The unassigned elements are recomputed before the wrapped graph's update, outside of its reentrancy guard,
    // and their events come back here while changedElementList is iterated.
    private boolean isUpdating;

    ListChainVariableReferenceGraph(
            AbstractVariableReferenceGraph<Solution_, ?> innerGraph,
            ListChainUpdater<Solution_> chainUpdater,
            ListVariableState<Solution_, Object, Object> listVariableState,
            Class<?> elementEntityClass,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> elementDescriptorList,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        this.innerGraph = innerGraph;
        this.chainUpdater = chainUpdater;
        this.listVariableState = listVariableState;
        this.elementEntityClass = elementEntityClass;
        this.changedVariableNotifier = changedVariableNotifier;
        this.variableReferenceToHookMap = new HashMap<>();
        this.changedElementList = new ArrayList<>();
        this.recomputedUnassignedElementMap = new ShrinkingIdentityHashMap<>();
        this.dirtyChainStateList = new ArrayList<>();
        this.isUpdating = false;

        this.monitoredSourceVariableSet = new HashSet<>();
        for (var descriptor : elementDescriptorList) {
            for (var source : descriptor.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    monitoredSourceVariableSet.add(sourceReference.variableMetaModel());
                }
            }
        }

        // Every element gets an initial computation, on the first update, and starts consistent;
        // its consistency can only change when its entity becomes inconsistent.
        for (var entity : entities) {
            if (elementEntityClass.isInstance(entity)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, entity, false);
                recordChangedElement(entity);
            }
        }
    }

    @Override
    public @Nullable VariableChangeHook resolveHookFor(VariableMetaModel<?, ?, ?> variableReference) {
        return variableReferenceToHookMap.computeIfAbsent(variableReference, this::buildHook);
    }

    private @Nullable VariableChangeHook buildHook(VariableMetaModel<?, ?, ?> variableReference) {
        var innerHook = innerGraph.resolveHookFor(variableReference);
        var isMonitoredSource = monitoredSourceVariableSet.contains(variableReference);
        // The list variable always has a hook in the graph: its chain nodes are registered under it.
        if (innerHook == null && !isMonitoredSource) {
            return null;
        }
        return new ListChainVariableChangeHook(innerHook, isMonitoredSource);
    }

    @Override
    public boolean hasPendingChanges() {
        // A dirty chain has its chain node marked in the graph.
        return !changedElementList.isEmpty() || innerGraph.hasPendingChanges();
    }

    @Override
    public boolean updateChanged() {
        isUpdating = true;
        classifyChangedElements();
        var isUpdated = innerGraph.updateChanged();
        endUpdate();
        isUpdating = false;
        return isUpdated;
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        // The wrapped graph is fixed, so it has none.
        return innerGraph.getVariableLoops();
    }

    private void recordChangedElement(Object element) {
        // An element's list variable state changes all at once, so its events come in a row.
        if (changedElementList.isEmpty() || changedElementList.getLast() != element) {
            changedElementList.add(element);
        }
    }

    /**
     * Classifies the recorded elements by the chain of their list entity.
     * An unassigned element has no chain node to walk it, so it is recomputed here.
     */
    private void classifyChangedElements() {
        for (var element : changedElementList) {
            if (!(listVariableState.getElementPosition(element) instanceof PositionInList positionInList)) {
                // A move changing an element before unassigning it records it twice, apart.
                if (recomputedUnassignedElementMap.put(element, Boolean.TRUE) == null) {
                    chainUpdater.recomputeUnassignedElement(element, changedVariableNotifier);
                }
                continue;
            }
            var chainState = chainUpdater.getChainState(positionInList.entity());
            chainState.addChangedElementIndex(positionInList.index());
            markChainDirty(chainState);
        }
        changedElementList.clear();
        recomputedUnassignedElementMap.clear();
    }

    private void markChainDirty(ListChainUpdater.ChainState<Solution_> chainState) {
        if (chainState.markDirty()) {
            dirtyChainStateList.add(chainState);
            innerGraph.markChanged(chainState.chainNode());
        }
    }

    private void endUpdate() {
        for (var chainState : dirtyChainStateList) {
            chainState.endUpdate();
        }
        dirtyChainStateList.clear();
    }

    /**
     * Records the changes the chain nodes need, then passes the event on to the graph.
     */
    private final class ListChainVariableChangeHook implements VariableChangeHook {

        private final @Nullable VariableChangeHook innerHook;
        private final boolean isMonitoredSource;

        private ListChainVariableChangeHook(@Nullable VariableChangeHook innerHook, boolean isMonitoredSource) {
            this.innerHook = innerHook;
            this.isMonitoredSource = isMonitoredSource;
        }

        @Override
        public void beforeVariableChanged(Object entity) {
            if (isUpdating) {
                // A reentrant event of this graph's own update.
                return;
            }
            if (innerHook != null) {
                innerHook.beforeVariableChanged(entity);
            }
        }

        @Override
        public void afterVariableChanged(Object entity) {
            if (isUpdating) {
                return;
            }
            // A pre-chain variable of the list entity, also a source, only changes during an update.
            if (isMonitoredSource && elementEntityClass.isInstance(entity)) {
                recordChangedElement(entity);
            }
            if (innerHook != null) {
                innerHook.afterVariableChanged(entity);
            }
        }

        @Override
        public void beforeListVariableChanged(Object entity, List<Object> elementsBeforeChange, int fromIndex,
                int toIndex) {
            // Nothing is recorded here: the list variable state changes the inverse and the previous or next
            // element of the elements that leave the list, and afterVariableChanged records them from those.
            if (innerHook != null) {
                innerHook.beforeListVariableChanged(entity, elementsBeforeChange, fromIndex, toIndex);
            }
        }

        @Override
        public void afterListVariableChanged(Object entity, List<Object> elementsAfterChange, int fromIndex,
                int toIndex) {
            // The graph's list element locators mark the list entity's post-chain variables changed.
            if (innerHook != null) {
                innerHook.afterListVariableChanged(entity, elementsAfterChange, fromIndex, toIndex);
            }
            if (fromIndex < toIndex) {
                // The elements whose source variables changed record themselves; when none of them is in this list,
                // as when a forced update of every shadow variable simulates a change on every list,
                // the whole chain is walked.
                markChainDirty(chainUpdater.getChainState(entity));
            }
        }
    }
}
