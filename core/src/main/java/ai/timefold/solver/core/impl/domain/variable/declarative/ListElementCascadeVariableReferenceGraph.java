package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.api.score.analysis.VariableLoop;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A variable reference graph that excludes a planning list variable's elements from the graph
 * and updates them by a cascade instead;
 * see {@link GraphStructure.GraphStructureAndDirection#cascadedElementClass()}.
 * <p>
 * The inner graph covers everything but the elements and is built by the normal machinery.
 * The cascade walks each dirty entity's list from the earliest dirty element,
 * and marks the entity's post-chain variables changed in the inner graph when an element changed.
 * When a pre-chain variable read by the elements changes during an inner update,
 * the notifier wrapper flags its entity for a whole-chain walk.
 * {@link #updateChanged()} alternates the cascade and the inner graph until neither has work left,
 * which terminates because the flagged values converge
 * (or stop changing once the inner graph marks their entities inconsistent).
 * <p>
 * The element suppliers run once per update, since a flagged entity's walk is deferred
 * until its pre-chain variables have settled. The flagged entity's post-chain variables
 * are the one exception: the inner update that changes the pre-chain variable recomputes
 * them in the same topological pass, through their pre-chain edge and before the cascade
 * has re-walked the chain they depend on, and they are recomputed once more after the walk
 * when it changed an element. This costs one extra supplier call per flagged entity and
 * update; {@code ListElementCascadeVariableReferenceGraphTest} pins both the single element
 * computations and this residual double evaluation.
 */
@NullMarked
public final class ListElementCascadeVariableReferenceGraph<Solution_> implements VariableReferenceGraph {

    private final VariableReferenceGraph innerGraph;
    private final @Nullable AbstractVariableReferenceGraph<Solution_, ?> innerNodeGraph;

    private final VariableUpdaterInfo<Solution_>[] elementUpdaters;
    private final Class<?> elementEntityClass;
    private final EntityConsistencyState<Solution_, Object> elementConsistencyState;
    private final @Nullable EntityConsistencyState<Solution_, Object> ownerConsistencyState;
    private final List<VariableMetaModel<?, ?, ?>> postChainVariableIdList;

    /**
     * Filled by the wrapped notifier when a pre-chain variable read by the elements changes;
     * see {@link DefaultShadowVariableSessionFactory#buildListElementCascadeGraph}.
     */
    private final Set<Object> wholeChainOwnerSet;

    private final UnaryOperator<@Nullable Object> nextInChain;
    private final UnaryOperator<@Nullable Object> elementToOwner;
    private final Comparator<Object> chainOrderComparator;
    private final Function<Object, @Nullable Object> ownerToFirstElement;

    private final Set<VariableMetaModel<?, ?, ?>> monitoredSourceVariableSet;
    private final ChangedVariableNotifier<Solution_> changedVariableNotifier;
    private final boolean canTerminateEarly;

    private final List<Object> changedElementList;
    private boolean isProcessing;

    @SuppressWarnings("unchecked")
    ListElementCascadeVariableReferenceGraph(
            ConsistencyTracker<Solution_> consistencyTracker,
            VariableReferenceGraph innerGraph,
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedElementDescriptorList,
            Class<?> elementEntityClass,
            @Nullable EntityConsistencyState<Solution_, Object> ownerConsistencyState,
            List<VariableMetaModel<?, ?, ?>> postChainVariableIdList,
            Set<Object> wholeChainOwnerSet,
            TopologicalSorter topologicalSorter,
            Function<Object, @Nullable Object> ownerToFirstElement,
            boolean canTerminateEarly,
            ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Object[] entities) {
        this.innerGraph = innerGraph;
        this.innerNodeGraph = innerGraph instanceof AbstractVariableReferenceGraph<?, ?> abstractGraph
                ? (AbstractVariableReferenceGraph<Solution_, ?>) abstractGraph
                : null;
        this.elementEntityClass = elementEntityClass;
        this.ownerConsistencyState = ownerConsistencyState;
        this.postChainVariableIdList = postChainVariableIdList;
        this.wholeChainOwnerSet = wholeChainOwnerSet;
        this.nextInChain = topologicalSorter.successor();
        this.elementToOwner = topologicalSorter.key();
        this.chainOrderComparator = topologicalSorter.comparator();
        this.ownerToFirstElement = ownerToFirstElement;
        this.canTerminateEarly = canTerminateEarly;
        this.changedVariableNotifier = changedVariableNotifier;
        this.changedElementList = new ArrayList<>();
        this.isProcessing = false;

        this.elementConsistencyState = consistencyTracker
                .getDeclarativeEntityConsistencyState(sortedElementDescriptorList.getFirst().getEntityDescriptor());
        this.monitoredSourceVariableSet = new HashSet<>();
        this.elementUpdaters = new VariableUpdaterInfo[sortedElementDescriptorList.size()];
        var updaterId = 0;
        for (var descriptor : sortedElementDescriptorList) {
            for (var source : descriptor.getSources()) {
                for (var sourceReference : source.variableSourceReferences()) {
                    monitoredSourceVariableSet.add(sourceReference.variableMetaModel());
                }
            }
            elementUpdaters[updaterId] = new VariableUpdaterInfo<>(
                    descriptor.getVariableMetaModel(), updaterId, descriptor, elementConsistencyState,
                    descriptor.getMemberAccessor(), descriptor.getCalculator());
            updaterId++;
        }

        // Every element gets an initial computation and starts consistent;
        // its consistency can only change when its owner becomes inconsistent.
        for (var entity : entities) {
            if (elementEntityClass.isInstance(entity)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, entity, false);
                changedElementList.add(entity);
            }
        }
        // The inner graph computes the pre-chain variables the first cascade reads.
        innerGraph.updateChanged();
        updateChanged();
    }

    @Override
    public void beforeVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity) {
        if (isProcessing) {
            // A reentrant event of this graph's own update; the notifier wrapper already observed it.
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
            changedElementList.add(entity);
        }
        innerGraph.afterVariableChanged(variableReference, entity);
    }

    @Override
    public void beforeListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        if (isProcessing) {
            throw new IllegalStateException("Impossible state: list variable changed during shadow variable update.");
        }
        innerGraph.beforeListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
    }

    @Override
    public void afterListVariableChanged(VariableMetaModel<?, ?, ?> variableReference, Object entity,
            List<Object> elementList, int fromIndex, int toIndex) {
        if (isProcessing) {
            throw new IllegalStateException("Impossible state: list variable changed during shadow variable update.");
        }
        for (var elementIndex = fromIndex; elementIndex < toIndex; elementIndex++) {
            changedElementList.add(elementList.get(elementIndex));
        }
        innerGraph.afterListVariableChanged(variableReference, entity, elementList, fromIndex, toIndex);
        // Stands in for the marking the inner graph does for a non-cascaded model, at the same
        // event: the element cascade skips the list element locators, hence also the mark that
        // comes with them. Without it, removing the list's last element would leave no element
        // to walk and no edge to the owner, so nothing would recompute its post-chain variables.
        markPostChainVariablesChanged(entity);
    }

    @Override
    public boolean updateChanged() {
        isProcessing = true;
        try {
            // Classify the changed elements into per-owner dirty ranges;
            // an unassigned element is reset immediately,
            // so re-assigning it to the same position is detected as a change.
            var ownerToDirtyChainStart = new IdentityHashMap<Object, Object>();
            var ownerToDirtyChainEnd = new IdentityHashMap<Object, Object>();
            for (var element : changedElementList) {
                var owner = elementToOwner.apply(element);
                if (owner == null) {
                    if (!elementConsistencyState.isEntityConsistent(element)) {
                        elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, element, false);
                    }
                    for (var updater : elementUpdaters) {
                        updater.updateIfChanged(element, changedVariableNotifier);
                    }
                    continue;
                }
                var dirtyChainStart = ownerToDirtyChainStart.get(owner);
                if (dirtyChainStart == null || chainOrderComparator.compare(element, dirtyChainStart) < 0) {
                    ownerToDirtyChainStart.put(owner, element);
                }
                var dirtyChainEnd = ownerToDirtyChainEnd.get(owner);
                if (dirtyChainEnd == null || chainOrderComparator.compare(element, dirtyChainEnd) > 0) {
                    ownerToDirtyChainEnd.put(owner, element);
                }
            }
            changedElementList.clear();

            // Alternate the cascade and the inner graph until neither has work left.
            // An owner whose pre-chain variables changed in the latest inner update is deferred
            // while other owners still need walking: those walks may change its pre-chain
            // variables again, and deferring keeps every element at a single computation.
            var pendingOwnerSet = Collections.newSetFromMap(new IdentityHashMap<>());
            pendingOwnerSet.addAll(ownerToDirtyChainStart.keySet());
            var wholeChainSet = Collections.newSetFromMap(new IdentityHashMap<>());
            var freshlyFlaggedOwnerList = drainWholeChainOwners();
            pendingOwnerSet.addAll(freshlyFlaggedOwnerList);
            wholeChainSet.addAll(freshlyFlaggedOwnerList);
            while (true) {
                if (!pendingOwnerSet.isEmpty()) {
                    var deferredOwnerSet = Collections.newSetFromMap(new IdentityHashMap<>());
                    deferredOwnerSet.addAll(freshlyFlaggedOwnerList);
                    var walkedOwnerList = new ArrayList<>();
                    for (var owner : pendingOwnerSet) {
                        if (deferredOwnerSet.size() < pendingOwnerSet.size() && deferredOwnerSet.contains(owner)) {
                            continue;
                        }
                        walkedOwnerList.add(owner);
                        walkOrMarkChainInconsistent(owner,
                                ownerToDirtyChainStart.remove(owner), ownerToDirtyChainEnd.remove(owner),
                                wholeChainSet.contains(owner));
                    }
                    walkedOwnerList.forEach(pendingOwnerSet::remove);
                }
                if (!innerGraph.updateChanged()) {
                    // The inner graph gave up on a structurally flawed solution and left its variables stale,
                    // so walking the remaining chains would only spread that staleness.
                    // Their work is put back, because the caller retries the update after undoing the move.
                    deferPendingOwners(pendingOwnerSet, ownerToDirtyChainStart, ownerToDirtyChainEnd, wholeChainSet);
                    return false;
                }
                freshlyFlaggedOwnerList = drainWholeChainOwners();
                if (freshlyFlaggedOwnerList.isEmpty() && pendingOwnerSet.isEmpty()) {
                    return true;
                }
                pendingOwnerSet.addAll(freshlyFlaggedOwnerList);
                wholeChainSet.addAll(freshlyFlaggedOwnerList);
            }
        } finally {
            isProcessing = false;
        }
    }

    /**
     * Restores the state the classification phase of {@link #updateChanged()} consumed,
     * so that a later update walks the chains that this one did not get to.
     */
    private void deferPendingOwners(Set<Object> pendingOwnerSet, Map<Object, Object> ownerToDirtyChainStart,
            Map<Object, Object> ownerToDirtyChainEnd, Set<Object> wholeChainSet) {
        for (var owner : pendingOwnerSet) {
            // The classification recomputes the dirty range as the extremes of the changed elements,
            // so the bounds alone describe the same range.
            var dirtyChainStart = ownerToDirtyChainStart.get(owner);
            if (dirtyChainStart != null) {
                changedElementList.add(dirtyChainStart);
                changedElementList.add(Objects.requireNonNull(ownerToDirtyChainEnd.get(owner)));
            }
            if (wholeChainSet.contains(owner)) {
                wholeChainOwnerSet.add(owner);
            }
        }
    }

    private List<Object> drainWholeChainOwners() {
        if (wholeChainOwnerSet.isEmpty()) {
            return Collections.emptyList();
        }
        var drainedOwnerList = new ArrayList<>(wholeChainOwnerSet);
        wholeChainOwnerSet.clear();
        return drainedOwnerList;
    }

    private void walkOrMarkChainInconsistent(Object owner, @Nullable Object dirtyChainStart,
            @Nullable Object dirtyChainEnd, boolean walkWholeChain) {
        var isOwnerInconsistent = ownerConsistencyState != null
                && Boolean.TRUE.equals(ownerConsistencyState.getEntityInconsistentValue(owner));
        if (isOwnerInconsistent) {
            markChainInconsistent(owner);
            return;
        }
        var anyElementChanged = walkChain(owner, dirtyChainStart, dirtyChainEnd, walkWholeChain);
        if (anyElementChanged) {
            markPostChainVariablesChanged(owner);
        }
    }

    private boolean walkChain(Object owner, @Nullable Object dirtyChainStart, @Nullable Object dirtyChainEnd,
            boolean walkWholeChain) {
        var chainStart = dirtyChainStart;
        if (walkWholeChain) {
            var firstElement = ownerToFirstElement.apply(owner);
            if (firstElement != null
                    && (chainStart == null || chainOrderComparator.compare(firstElement, chainStart) < 0)) {
                chainStart = firstElement;
            }
        }
        if (chainStart == null) {
            return false;
        }
        var anyElementChangedInWalk = false;
        var current = chainStart;
        while (current != null) {
            if (!elementConsistencyState.isEntityConsistent(current)) {
                // The element's owner recovered from a dependency loop.
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, current, false);
            }
            var anyElementVariableChanged = false;
            for (var updater : elementUpdaters) {
                anyElementVariableChanged |= updater.updateIfChanged(current, changedVariableNotifier);
            }
            anyElementChangedInWalk |= anyElementVariableChanged;
            if (canTerminateEarly && !walkWholeChain && !anyElementVariableChanged
            // A swap can create multiple non-contiguous dirty elements on the same chain,
            // so only terminate early once the last dirty element has been reached.
                    && (dirtyChainEnd == null || chainOrderComparator.compare(current, dirtyChainEnd) >= 0)) {
                break;
            }
            current = nextInChain.apply(current);
        }
        return anyElementChangedInWalk;
    }

    private void markChainInconsistent(Object owner) {
        // The owner is part of a dependency loop the solver may break later;
        // its elements read its pre-chain variables, so they are inconsistent with it.
        var current = ownerToFirstElement.apply(owner);
        while (current != null) {
            if (elementConsistencyState.isEntityConsistent(current)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, current, true);
            }
            for (var updater : elementUpdaters) {
                updater.updateIfChanged(current, null, changedVariableNotifier);
            }
            current = nextInChain.apply(current);
        }
    }

    @Override
    public List<VariableLoop> getVariableLoops() {
        // The elements are only excluded from the inner graph when their sources form a chain
        // fed by the owner's pre-chain variables, so they can never be part of a loop themselves.
        return innerGraph.getVariableLoops();
    }

    private void markPostChainVariablesChanged(Object owner) {
        if (innerNodeGraph == null) {
            return;
        }
        for (var variableId : postChainVariableIdList) {
            var node = innerNodeGraph.lookupOrNull(variableId, owner);
            if (node != null) {
                innerNodeGraph.markChanged(node);
            }
        }
    }
}
