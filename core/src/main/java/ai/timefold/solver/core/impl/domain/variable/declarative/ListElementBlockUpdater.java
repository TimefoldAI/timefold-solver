package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Updates the declarative shadow variables of a planning list variable's elements,
 * which are excluded from the variable reference graph and represented by one block node
 * per list entity instead; see {@link GraphStructure#LIST_ELEMENT_BLOCK}.
 * <p>
 * A single instance backs every list entity's block node.
 * When a block node is processed, {@link #update(Object, boolean, ChangedVariableNotifier)}
 * walks the entity's chain from the earliest dirty element and reports whether anything
 * changed, which propagates to the entity's post-chain variables through the graph's edges.
 * The dirty ranges are maintained by {@link ListElementBlockVariableReferenceGraph}
 * from the list variable's change events and the elements' source variable changes.
 * An entity with no dirty range has its whole chain walked.
 * <p>
 * When the block node is part of a dependency loop, the elements follow their entity:
 * they are marked inconsistent and their variables are set to null.
 */
@NullMarked
final class ListElementBlockUpdater<Solution_> implements VariableUpdater<Solution_> {

    private final VariableMetaModel<Solution_, ?, ?> listVariableMetaModel;
    private final EntityConsistencyState<Solution_, Object> ownerConsistencyState;
    private final EntityConsistencyState<Solution_, Object> elementConsistencyState;
    private final VariableUpdaterInfo<Solution_>[] elementUpdaters;

    private final UnaryOperator<@Nullable Object> nextInChain;
    private final UnaryOperator<Object> elementToOwner;
    private final Comparator<Object> chainOrderComparator;
    private final Function<Object, @Nullable Object> ownerToFirstElement;
    private final boolean canTerminateEarly;

    // Mutable dirty state, written by ListElementBlockVariableReferenceGraph
    // and by the notifier wrapper created in DefaultShadowVariableSessionFactory.
    // An owner absent from these maps has its whole chain walked.
    private final List<Object> changedElementList;
    private final IdentityHashMap<Object, Object> ownerToDirtyChainStart;
    private final IdentityHashMap<Object, Object> ownerToDirtyChainEnd;

    @SuppressWarnings("unchecked")
    ListElementBlockUpdater(
            VariableMetaModel<Solution_, ?, ?> listVariableMetaModel,
            EntityConsistencyState<Solution_, Object> ownerConsistencyState,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedElementDescriptorList,
            TopologicalSorter topologicalSorter,
            Function<Object, @Nullable Object> ownerToFirstElement,
            boolean canTerminateEarly) {
        this.listVariableMetaModel = listVariableMetaModel;
        this.ownerConsistencyState = ownerConsistencyState;
        this.elementConsistencyState = elementConsistencyState;
        this.nextInChain = topologicalSorter.successor();
        this.elementToOwner = topologicalSorter.key();
        this.chainOrderComparator = topologicalSorter.comparator();
        this.ownerToFirstElement = ownerToFirstElement;
        this.canTerminateEarly = canTerminateEarly;
        this.changedElementList = new ArrayList<>();
        this.ownerToDirtyChainStart = new IdentityHashMap<>();
        this.ownerToDirtyChainEnd = new IdentityHashMap<>();

        this.elementUpdaters = new VariableUpdaterInfo[sortedElementDescriptorList.size()];
        var updaterId = 0;
        for (var descriptor : sortedElementDescriptorList) {
            elementUpdaters[updaterId] = new VariableUpdaterInfo<>(
                    descriptor.getVariableMetaModel(), updaterId, descriptor, elementConsistencyState,
                    descriptor.getMemberAccessor(), descriptor.getCalculator());
            updaterId++;
        }
    }

    @Override
    public VariableMetaModel<Solution_, ?, ?> id() {
        return listVariableMetaModel;
    }

    @Override
    public Object nodeGroupKey() {
        // One block node per list entity, which is also how the graph looks them up;
        // a metamodel never collides with the other updaters, whose keys are their group ids.
        return listVariableMetaModel;
    }

    @Override
    public @Nullable Object[] groupEntities() {
        return null;
    }

    @Override
    public EntityConsistencyState<Solution_, Object> entityConsistencyState() {
        return ownerConsistencyState;
    }

    @Override
    public boolean update(Object owner, boolean isEntityInconsistent,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var chainStart = ownerToDirtyChainStart.remove(owner);
        var dirtyChainEnd = ownerToDirtyChainEnd.remove(owner);
        if (isEntityInconsistent) {
            // The owner is part of a dependency loop the solver may break later;
            // its elements read its pre-chain variables, so they are inconsistent with it.
            return markChainInconsistent(owner, changedVariableNotifier);
        }
        var firstElement = ownerToFirstElement.apply(owner);
        if (chainStart == null
                || (firstElement != null && !elementConsistencyState.isEntityConsistent(firstElement))) {
            // Nothing was recorded, or the owner recovered from a dependency loop that left its
            // whole chain inconsistent; either way the chain is walked from its first element.
            chainStart = firstElement;
            dirtyChainEnd = null;
        }
        return walkChain(chainStart, dirtyChainEnd, changedVariableNotifier);
    }

    private boolean walkChain(@Nullable Object chainStart, @Nullable Object dirtyChainEnd,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        if (chainStart == null) {
            return false;
        }
        var anyElementChangedInWalk = false;
        var current = chainStart;
        var seenDirtyChainEnd = false;
        while (current != null) {
            if (!elementConsistencyState.isEntityConsistent(current)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, current, false);
            }
            var anyElementVariableChanged = false;
            for (var updater : elementUpdaters) {
                anyElementVariableChanged |= updater.updateIfChanged(current, changedVariableNotifier);
            }
            anyElementChangedInWalk |= anyElementVariableChanged;
            seenDirtyChainEnd |= current == dirtyChainEnd;
            // A swap can leave non-contiguous dirty elements, so stop only once the last one is
            // reached; when dirtyChainEnd is null (whole chain), current == dirtyChainEnd can
            // never hold here, so seenDirtyChainEnd stays false and the walk never stops early.
            if (canTerminateEarly && !anyElementVariableChanged && seenDirtyChainEnd) {
                break;
            }
            current = nextInChain.apply(current);
        }
        return anyElementChangedInWalk;
    }

    private boolean markChainInconsistent(Object owner, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var anyElementChanged = false;
        var current = ownerToFirstElement.apply(owner);
        while (current != null) {
            if (elementConsistencyState.isEntityConsistent(current)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, current, true);
            }
            for (var updater : elementUpdaters) {
                anyElementChanged |= updater.updateIfChanged(current, null, changedVariableNotifier);
            }
            current = nextInChain.apply(current);
        }
        return anyElementChanged;
    }

    /**
     * Records an element whose source variables changed, or that may have left its list;
     * classified into a per-owner dirty range by {@link #classifyChangedElements}.
     */
    void recordChangedElement(Object element) {
        changedElementList.add(element);
    }

    /**
     * Records that the whole chain of the given entity must be walked, because a variable its
     * elements read through their inverse changed during the update.
     */
    void recordWholeChainDirty(Object owner) {
        ownerToDirtyChainStart.remove(owner);
        ownerToDirtyChainEnd.remove(owner);
    }

    /**
     * Classifies the recorded elements into per-owner dirty ranges and feeds each dirty owner
     * to the given consumer, so its block node can be marked changed.
     * An unassigned element is recomputed here rather than by a block node, having no list entity;
     * its suppliers read a null inverse, so it ends up cleared,
     * and re-assigning it to the same position is detected as a change.
     */
    void classifyChangedElements(ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Consumer<Object> dirtyOwnerConsumer) {
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
        // Marking is a bit in a set (or a slot in a topologically ordered queue) either way,
        // so the order these identity maps happen to iterate in does not reach the result.
        for (var owner : ownerToDirtyChainStart.keySet()) {
            dirtyOwnerConsumer.accept(owner);
        }
    }

    /**
     * Clears the flags a block node did not consume during the update,
     * e.g. when its entity was removed from the working solution.
     */
    void clearTransientState() {
        ownerToDirtyChainStart.clear();
        ownerToDirtyChainEnd.clear();
    }
}
