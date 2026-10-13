package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Updates the declarative shadow variables of a planning list variable's elements,
 * which are excluded from the variable reference graph and represented by one chain node
 * per list entity instead; see {@link GraphStructure#LIST_CHAIN}.
 * <p>
 * A single instance backs every list entity's chain node.
 * When a chain node is processed, {@link #update(Object, boolean, ChangedVariableNotifier)}
 * walks the entity's chain the way {@link SingleDirectionalParentVariableReferenceGraph} does:
 * from each element whose source variables changed, in chain order, until an element is unchanged.
 * It reports whether anything changed, which propagates to the entity's post-chain variables
 * through the graph's edges.
 * The whole chain is walked when a pre-chain variable changed, since any element may read it,
 * and when no element recorded where the chain changed.
 * Each list entity's {@link ChainState} is allocated once, here;
 * {@link ListChainVariableReferenceGraph}, which tracks the changes, records in it where the chain changed.
 */
@NullMarked
final class ListChainUpdater<Solution_> implements VariableUpdater<Solution_> {

    // These are immutable.
    private final VariableMetaModel<Solution_, ?, ?> listVariableMetaModel;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    // A previous element parent orders the chain as the list; a next element parent reverses it.
    private final boolean isChainInListOrder;
    private final EntityConsistencyState<Solution_, Object> listEntityConsistencyState;
    private final VariableUpdaterInfo<Solution_>[] elementUpdaters;
    // The list entity's declarative variables its elements read through their inverse.
    private final VariableDescriptor<Solution_>[] preChainVariableDescriptors;
    private final boolean canTerminateEarly;

    // The chain states are mutable, written by ListChainVariableReferenceGraph as it records the changes
    // and by this updater as it walks the chains.
    private final IdentityHashMap<Object, ChainState<Solution_>> listEntityToChainStateMap;

    @SuppressWarnings("unchecked")
    ListChainUpdater(
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            boolean isChainInListOrder,
            EntityConsistencyState<Solution_, Object> listEntityConsistencyState,
            EntityConsistencyState<Solution_, Object> elementConsistencyState,
            List<DeclarativeShadowVariableDescriptor<Solution_>> sortedElementDescriptorList,
            List<VariableDescriptor<Solution_>> preChainVariableDescriptorList,
            boolean canTerminateEarly) {
        this.listVariableMetaModel = listVariableDescriptor.getVariableMetaModel();
        this.preChainVariableDescriptors = preChainVariableDescriptorList.toArray(new VariableDescriptor[0]);
        this.listVariableDescriptor = listVariableDescriptor;
        this.isChainInListOrder = isChainInListOrder;
        this.listEntityConsistencyState = listEntityConsistencyState;
        this.canTerminateEarly = canTerminateEarly;
        this.listEntityToChainStateMap = new IdentityHashMap<>();

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
        // One chain node per list entity, which is also how the graph looks them up;
        // a metamodel never collides with the other updaters, whose keys are their group ids.
        return listVariableMetaModel;
    }

    @Override
    public @Nullable Object[] groupEntities() {
        return null;
    }

    @Override
    public EntityConsistencyState<Solution_, Object> entityConsistencyState() {
        return listEntityConsistencyState;
    }

    @Override
    public boolean update(Object listEntity, boolean isEntityInconsistent,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        // A chain node is only ever part of a fixed graph, which has no dependency loop
        // and therefore never reports a list entity inconsistent.
        // The list cannot change while the graph updates, so it is read once.
        var elementList = listVariableDescriptor.getValue(listEntity);
        var chainState = listEntityToChainStateMap.get(listEntity);
        // The declarative pre-chain nodes come before the chain node in the graph's order,
        // so they are already up to date for this update.
        var isPreChainChanged = updatePreChainValues(listEntity, chainState);
        if (chainState.isChainStale || isPreChainChanged || chainState.changedElementCount == 0) {
            chainState.isChainStale = false;
            return walkWholeChain(elementList, changedVariableNotifier);
        }
        return walkFromChangedElements(elementList, chainState, changedVariableNotifier);
    }

    private boolean walkWholeChain(List<Object> elementList, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var anyElementChanged = false;
        for (var position = 0; position < elementList.size(); position++) {
            anyElementChanged |= updateElement(elementAt(elementList, position), changedVariableNotifier);
        }
        return anyElementChanged;
    }

    private boolean walkFromChangedElements(List<Object> elementList, ChainState<Solution_> chainState,
            ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        var changedElementIndexes = chainState.changedElementIndexes;
        var changedElementCount = chainState.changedElementCount;
        Arrays.sort(changedElementIndexes, 0, changedElementCount);
        var chainLength = elementList.size();
        var anyElementChanged = false;
        // The changed elements up to it have been walked after their predecessors,
        // so walking them again changes nothing.
        var lastWalkedPosition = -1;
        for (var i = 0; i < changedElementCount; i++) {
            var position = isChainInListOrder ? changedElementIndexes[i]
                    : chainLength - 1 - changedElementIndexes[changedElementCount - 1 - i];
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

    private Object elementAt(List<Object> elementList, int position) {
        return elementList.get(isChainInListOrder ? position : elementList.size() - 1 - position);
    }

    /**
     * @return true if a pre-chain variable differs from the value the chain was last walked with
     */
    private boolean updatePreChainValues(Object listEntity, ChainState<Solution_> chainState) {
        var isChanged = false;
        for (var i = 0; i < preChainVariableDescriptors.length; i++) {
            var value = preChainVariableDescriptors[i].getValue(listEntity);
            if (!Objects.equals(chainState.preChainValues[i], value)) {
                chainState.preChainValues[i] = value;
                isChanged = true;
            }
        }
        return isChanged;
    }

    /**
     * Recomputes an element that left its list, which no chain node walks;
     * its suppliers read a null inverse, so it ends up cleared,
     * and re-assigning it to the same position is detected as a change.
     */
    void recomputeUnassignedElement(Object element, ChangedVariableNotifier<Solution_> changedVariableNotifier) {
        updateElement(element, changedVariableNotifier);
    }

    /**
     * @return null for anything but a list entity of the working solution the graph was built for
     */
    @Nullable
    ChainState<Solution_> getChainState(Object listEntity) {
        return listEntityToChainStateMap.get(listEntity);
    }

    /**
     * Registers a list entity whose chain node this updater backs.
     */
    void addListEntity(Object listEntity, GraphNode<Solution_> chainNode) {
        listEntityToChainStateMap.put(listEntity, new ChainState<>(chainNode, preChainVariableDescriptors.length));
    }

    /**
     * A list entity's chain node and the dirty state of its chain.
     */
    static final class ChainState<Solution_> {

        private final GraphNode<Solution_> chainNode;
        // The list entity's pre-chain values the chain was last walked with; not reset between updates.
        private final @Nullable Object[] preChainValues;
        // The list indexes of the elements whose source variables changed, in no particular order.
        private int[] changedElementIndexes = new int[4];
        private int changedElementCount;
        // Changed since the last update, so in the graph's dirtyChainStateList.
        private boolean isDirty;
        // The chain was never walked, until an update walks it whole.
        private boolean isChainStale = true;

        private ChainState(GraphNode<Solution_> chainNode, int preChainVariableCount) {
            this.chainNode = chainNode;
            this.preChainValues = new Object[preChainVariableCount];
        }

        GraphNode<Solution_> chainNode() {
            return chainNode;
        }

        void addChangedElementIndex(int index) {
            if (changedElementCount == changedElementIndexes.length) {
                changedElementIndexes = Arrays.copyOf(changedElementIndexes, changedElementCount * 2);
            }
            changedElementIndexes[changedElementCount++] = index;
        }

        /**
         * @return true if the chain was not dirty yet
         */
        boolean markDirty() {
            if (isDirty) {
                return false;
            }
            isDirty = true;
            return true;
        }

        /**
         * Resets the chain an update dirtied.
         */
        void endUpdate() {
            changedElementCount = 0;
            isDirty = false;
        }
    }
}
