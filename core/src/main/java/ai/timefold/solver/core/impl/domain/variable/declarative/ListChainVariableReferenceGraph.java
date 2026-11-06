package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

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
 * The graph itself covers everything else and is built by the normal machinery,
 * with fixed edges from each entity's declarative pre-chain variables to its chain node
 * and from its chain node to its post-chain variables;
 * a single {@link #updateChanged()} pass in topological order therefore walks each dirty chain
 * exactly once, after its pre-chain variables and before its post-chain variables.
 * This wrapper tracks the changes the chain nodes need, like any graph tracks its changed nodes:
 * it records the elements whose source variables changed and the list entities whose list changed,
 * classifies the elements by the chain of their entity, into the chain state the updater walks from,
 * and marks the dirty entities' chain nodes before delegating the update.
 * It also marks the list entity's post-chain variables changed on a list change,
 * which the graph derives from the list element locators the chain node skips.
 * <p>
 * The classification runs before the delegated update, and recomputes the elements that left their
 * list along the way; those writes come back here through the score director. The graph's own
 * reentrancy guard does not cover that window, since it only spans the update it delegates to,
 * hence {@link #isUpdating}: without it the classification would add to the list it is iterating.
 */
@NullMarked
final class ListChainVariableReferenceGraph<Solution_> implements VariableReferenceGraph {

    // These are immutable.
    private final AbstractVariableReferenceGraph<Solution_, ?> innerGraph;
    private final ListChainUpdater<Solution_> chainUpdater;
    private final ListVariableState<Solution_, Object, Object> listVariableState;
    private final VariableMetaModel<Solution_, ?, ?> listVariableMetaModel;
    private final Class<?> elementEntityClass;
    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    /**
     * The list variable's after processors, which mark the list entity's post-chain variables changed.
     * Hoisted at construction to save a lookup by variable for every list change.
     */
    private final List<BiConsumer<AbstractVariableReferenceGraph<Solution_, ?>, Object>> listVariableAfterProcessorList;

    // These are mutable.
    // Resolved once per variable, as the graph it wraps does.
    private final Map<VariableMetaModel<?, ?, ?>, @Nullable VariableChangeHook> variableReferenceToHookMap;
    private final List<Object> changedElementList;
    // The unassigned elements the classification in progress recomputed, so that each is recomputed once.
    // Its clear() keeps the update cheap even after the initial update, which records every unassigned element.
    private final ShrinkingIdentityHashMap<Object, Boolean> recomputedUnassignedElementMap;
    private final List<ListChainUpdater.ChainState<Solution_>> dirtyChainStateList;
    private boolean isUpdating;

    ListChainVariableReferenceGraph(
            AbstractVariableReferenceGraph<Solution_, ?> innerGraph,
            ListChainUpdater<Solution_> chainUpdater,
            ListVariableState<Solution_, Object, Object> listVariableState,
            VariableMetaModel<Solution_, ?, ?> listVariableMetaModel,
            Class<?> elementEntityClass,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> elementDescriptorList,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        this.innerGraph = innerGraph;
        this.chainUpdater = chainUpdater;
        this.listVariableState = listVariableState;
        this.listVariableMetaModel = listVariableMetaModel;
        this.elementEntityClass = elementEntityClass;
        this.changedVariableNotifier = changedVariableNotifier;
        this.listVariableAfterProcessorList =
                innerGraph.variableReferenceToAfterProcessor.getOrDefault(listVariableMetaModel, List.of());
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
        var isListVariable = variableReference.equals(listVariableMetaModel);
        if (innerHook == null && !isMonitoredSource && !isListVariable) {
            return null;
        }
        return new ListChainVariableChangeHook(innerHook, isMonitoredSource, isListVariable);
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

    /**
     * Stands in for the mark the graph derives from the list element locators, which the chain node
     * skips along with the edges. Without it, removing the list's last element would leave no element
     * to walk and no edge to the entity, so nothing would recompute its post-chain variables.
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void markPostChainVariablesChanged(Object entity) {
        // Avoid creation of iterators on the hot path.
        for (var i = 0; i < listVariableAfterProcessorList.size(); i++) {
            listVariableAfterProcessorList.get(i).accept(innerGraph, entity);
        }
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
            var chainState = Objects.requireNonNull(chainUpdater.getChainState(positionInList.entity()));
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
        private final boolean isListVariable;

        private ListChainVariableChangeHook(@Nullable VariableChangeHook innerHook, boolean isMonitoredSource,
                boolean isListVariable) {
            this.innerHook = innerHook;
            this.isMonitoredSource = isMonitoredSource;
            this.isListVariable = isListVariable;
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
            if (innerHook != null) {
                innerHook.afterListVariableChanged(entity, elementsAfterChange, fromIndex, toIndex);
            }
            if (!isListVariable) {
                return;
            }
            if (fromIndex < toIndex) {
                // The elements whose source variables changed record themselves; when none of them is in this list,
                // as when a forced update of every shadow variable simulates a change on every list,
                // the whole chain is walked.
                markChainDirty(Objects.requireNonNull(chainUpdater.getChainState(entity)));
            }
            markPostChainVariablesChanged(entity);
        }
    }
}
