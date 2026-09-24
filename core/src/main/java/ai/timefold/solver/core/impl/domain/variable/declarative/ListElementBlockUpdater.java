package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.analysis.EntityVariablePair;
import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
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
 * walks the entity's chain the way {@link SingleDirectionalParentVariableReferenceGraph} does:
 * from each element whose source variables changed, in chain order, until an element is unchanged.
 * It reports whether anything changed, which propagates to the entity's post-chain variables
 * through the graph's edges.
 * The whole chain is walked when a pre-chain variable changed, since any element may read it,
 * and when no element recorded where the chain changed.
 * The dirty state of every list entity is allocated once;
 * after an update, only the entities that update dirtied are reset.
 * <p>
 * When the block node is part of a dependency loop, the elements follow their entity:
 * they are marked inconsistent and their variables are set to null.
 */
@NullMarked
final class ListElementBlockUpdater<Solution_> implements VariableUpdater<Solution_> {

    // These are immutable.
    private final VariableMetaModel<Solution_, ?, ?> listVariableMetaModel;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final ListVariableState<Solution_, Object, Object> listVariableState;
    // A previous element parent orders the chain as the list; a next element parent reverses it.
    private final boolean isChainInListOrder;
    private final EntityConsistencyState<Solution_, Object> ownerConsistencyState;
    private final EntityConsistencyState<Solution_, Object> elementConsistencyState;
    private final VariableUpdaterInfo<Solution_>[] elementUpdaters;
    // The list entity's variables its elements read through their inverse.
    private final DeclarativeShadowVariableDescriptor<Solution_>[] preChainVariableDescriptors;
    private final boolean canTerminateEarly;

    // These are mutable, written by ListElementBlockVariableReferenceGraph.
    private final List<Object> changedElementList;
    // The unassigned elements the classification in progress recomputed, so that each is recomputed once.
    private final List<Object> recomputedUnassignedElementList;
    private final IdentityHashMap<Object, ChainState> ownerToChainStateMap;
    private final List<ChainState> dirtyChainStateList;

    @SuppressWarnings("unchecked")
    ListElementBlockUpdater(
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            ListVariableState<Solution_, Object, Object> listVariableState,
            boolean isChainInListOrder,
            EntityConsistencyState<Solution_, Object> ownerConsistencyState,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedElementDescriptorList,
            List<DeclarativeShadowVariableDescriptor<Solution_>> preChainVariableDescriptorList,
            boolean canTerminateEarly) {
        this.listVariableMetaModel = listVariableDescriptor.getVariableMetaModel();
        this.preChainVariableDescriptors = preChainVariableDescriptorList.toArray(new DeclarativeShadowVariableDescriptor[0]);
        this.listVariableDescriptor = listVariableDescriptor;
        this.listVariableState = listVariableState;
        this.isChainInListOrder = isChainInListOrder;
        this.ownerConsistencyState = ownerConsistencyState;
        this.elementConsistencyState = elementConsistencyState;
        this.canTerminateEarly = canTerminateEarly;
        this.changedElementList = new ArrayList<>();
        this.recomputedUnassignedElementList = new ArrayList<>();
        this.ownerToChainStateMap = new IdentityHashMap<>();
        this.dirtyChainStateList = new ArrayList<>();

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
        // The list cannot change while the graph updates, so it is read once.
        var elementList = listVariableDescriptor.getValue(owner);
        var chainState = ownerToChainStateMap.get(owner);
        if (isEntityInconsistent) {
            // The owner is part of a dependency loop the solver may break later;
            // its elements read its pre-chain variables, so they are inconsistent with it.
            chainState.isChainStale = true;
            return markChainInconsistent(elementList, changedVariableNotifier);
        }
        // The pre-chain nodes come before the block node in the graph's order,
        // so they are already up to date for this update.
        var isPreChainChanged = updatePreChainValues(owner, chainState);
        if (chainState.isChainStale) {
            chainState.isChainStale = false;
            for (var element : elementList) {
                markConsistent(element, changedVariableNotifier);
            }
            return walkWholeChain(elementList, changedVariableNotifier);
        }
        // Outside a stale chain, an inconsistent element came from one since the last update. A dependency loop
        // only reaches a chain through the pre-chain variables its elements read through their inverse,
        // so the inverse is a source variable, and its change made the element a seed.
        for (var i = 0; i < chainState.seedCount; i++) {
            markConsistent(elementList.get(chainState.seedIndexes[i]), changedVariableNotifier);
        }
        if (isPreChainChanged || chainState.seedCount == 0) {
            return walkWholeChain(elementList, changedVariableNotifier);
        }
        return walkFromSeeds(elementList, chainState, changedVariableNotifier);
    }

    private boolean walkWholeChain(List<Object> elementList, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var anyElementChanged = false;
        for (var position = 0; position < elementList.size(); position++) {
            anyElementChanged |= updateElement(elementAt(elementList, position), changedVariableNotifier);
        }
        return anyElementChanged;
    }

    private boolean walkFromSeeds(List<Object> elementList, ChainState chainState,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var seedIndexes = chainState.seedIndexes;
        var seedCount = chainState.seedCount;
        Arrays.sort(seedIndexes, 0, seedCount);
        var chainLength = elementList.size();
        var anyElementChanged = false;
        // The seeds up to it have been walked after their predecessors, so walking them again changes nothing.
        var lastWalkedPosition = -1;
        for (var i = 0; i < seedCount; i++) {
            var position = isChainInListOrder ? seedIndexes[i] : chainLength - 1 - seedIndexes[seedCount - 1 - i];
            if (position <= lastWalkedPosition) {
                continue;
            }
            var isElementChanged = true;
            for (; position < chainLength && (isElementChanged || !canTerminateEarly); position++) {
                isElementChanged = updateElement(elementAt(elementList, position), changedVariableNotifier);
                anyElementChanged |= isElementChanged;
            }
            lastWalkedPosition = position - 1;
        }
        return anyElementChanged;
    }

    private boolean updateElement(Object element, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var isElementChanged = false;
        for (var updater : elementUpdaters) {
            isElementChanged |= updater.updateIfChanged(element, changedVariableNotifier);
        }
        return isElementChanged;
    }

    private void markConsistent(Object element, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        if (!elementConsistencyState.isEntityConsistent(element)) {
            elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, element, false);
        }
    }

    private boolean markChainInconsistent(List<Object> elementList,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var anyElementChanged = false;
        for (var position = 0; position < elementList.size(); position++) {
            var element = elementAt(elementList, position);
            if (elementConsistencyState.isEntityConsistent(element)) {
                elementConsistencyState.setEntityIsInconsistent(changedVariableNotifier, element, true);
            }
            for (var updater : elementUpdaters) {
                anyElementChanged |= updater.updateIfChanged(element, null, changedVariableNotifier);
            }
        }
        return anyElementChanged;
    }

    private Object elementAt(List<Object> elementList, int position) {
        return elementList.get(isChainInListOrder ? position : elementList.size() - 1 - position);
    }

    /**
     * @return true if a pre-chain variable differs from the value the chain was last walked with
     */
    private boolean updatePreChainValues(Object owner, ChainState chainState) {
        var isChanged = false;
        for (var i = 0; i < preChainVariableDescriptors.length; i++) {
            var value = preChainVariableDescriptors[i].getValue(owner);
            if (!Objects.equals(chainState.preChainValues[i], value)) {
                chainState.preChainValues[i] = value;
                isChanged = true;
            }
        }
        return isChanged;
    }

    /**
     * Records an element whose source variables changed;
     * classified into a seed of its owner's chain by {@link #classifyChangedElements}.
     */
    void recordChangedElement(Object element) {
        // An element's list variable state changes all at once, so its events come in a row.
        if (changedElementList.isEmpty() || changedElementList.getLast() != element) {
            changedElementList.add(element);
        }
    }

    /**
     * Records a list entity whose list changed.
     * The elements whose source variables changed record themselves; when none of them is in this list,
     * as when a forced update of every shadow variable simulates a change on every list,
     * the whole chain is walked.
     */
    void recordChangedList(Object owner) {
        markDirty(ownerToChainStateMap.get(owner));
    }

    /**
     * Adds every declarative variable of every element of the list entity's chain.
     */
    void addElementVariables(Object owner, Set<EntityVariablePair> entityVariablePairSet) {
        for (var element : listVariableDescriptor.getValue(owner)) {
            for (var updater : elementUpdaters) {
                entityVariablePairSet.add(new EntityVariablePair(element, updater.id().name()));
            }
        }
    }

    /**
     * Registers a list entity whose block node this updater backs.
     */
    void addListEntity(Object owner) {
        ownerToChainStateMap.put(owner, new ChainState(owner, preChainVariableDescriptors.length));
    }

    private void markDirty(ChainState chainState) {
        if (!chainState.isDirty) {
            chainState.isDirty = true;
            dirtyChainStateList.add(chainState);
        }
    }

    /**
     * Classifies the recorded elements into seeds of their owners' chains and feeds each dirty owner
     * to the given consumer, so its block node can be marked changed.
     * An unassigned element is recomputed here rather than by a block node, having no list entity;
     * its suppliers read a null inverse, so it ends up cleared,
     * and re-assigning it to the same position is detected as a change.
     */
    void classifyChangedElements(ChangedVariableNotifier<Solution_> changedVariableNotifier,
            Consumer<Object> dirtyOwnerConsumer) {
        for (var element : changedElementList) {
            var owner = listVariableState.getInverseSingleton(element);
            if (owner == null) {
                // A move changing an element before unassigning it records it twice, apart.
                if (!containsSame(recomputedUnassignedElementList, element)) {
                    recomputedUnassignedElementList.add(element);
                    markConsistent(element, changedVariableNotifier);
                    updateElement(element, changedVariableNotifier);
                }
                continue;
            }
            var chainState = ownerToChainStateMap.get(owner);
            chainState.addSeed(listVariableState.getIndexOrFail(element));
            markDirty(chainState);
        }
        changedElementList.clear();
        recomputedUnassignedElementList.clear();
        for (var chainState : dirtyChainStateList) {
            dirtyOwnerConsumer.accept(chainState.owner);
        }
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    private static boolean containsSame(List<Object> elementList, Object element) {
        // Avoid creation of iterators on the hot path.
        for (var i = 0; i < elementList.size(); i++) {
            if (elementList.get(i) == element) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resets the chains the update dirtied.
     * An update that gave up on a dependency loop processed no block node, and the graph keeps their
     * marks for the next update; their chains are walked whole then, because a legacy composite move
     * may change them again before that update, without undoing first.
     */
    void endUpdate(boolean isUpdated) {
        for (var chainState : dirtyChainStateList) {
            if (!isUpdated) {
                chainState.isChainStale = true;
            }
            chainState.reset();
        }
        dirtyChainStateList.clear();
    }

    /**
     * The dirty state of a list entity's chain.
     */
    private static final class ChainState {

        // Differs from any value, so that the first update walks the whole chain.
        private static final Object NOT_WALKED = new Object();

        private final Object owner;
        // The owner's pre-chain values the chain was last walked with; not reset between updates.
        private final @Nullable Object[] preChainValues;
        // The list indexes of the elements whose source variables changed, in no particular order.
        private int[] seedIndexes = new int[4];
        private int seedCount;
        // In dirtyChainStateList.
        private boolean isDirty;
        // A dependency loop marked the chain inconsistent, or an update gave up before walking it,
        // until an update walks the whole chain; not reset between updates.
        private boolean isChainStale;

        private ChainState(Object owner, int preChainVariableCount) {
            this.owner = owner;
            this.preChainValues = new Object[preChainVariableCount];
            Arrays.fill(preChainValues, NOT_WALKED);
        }

        private void addSeed(int index) {
            if (seedCount == seedIndexes.length) {
                seedIndexes = Arrays.copyOf(seedIndexes, seedCount * 2);
            }
            seedIndexes[seedCount++] = index;
        }

        private void reset() {
            seedCount = 0;
            isDirty = false;
        }
    }
}
