package ai.timefold.solver.core.impl.heuristic.selector.list;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.solver.random.RandomUtils;

final class ElementLocationRandomIterator<Solution_> implements Iterator<ElementLocation> {

    private final ListVariableStateSupply<Solution_> listVariableStateSupply;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final EntitySelector<Solution_> entitySelector;
    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final Iterator<Object> entityIterator;
    private final Random workingRandom;
    private final long totalSize;
    private final boolean allowsUnassignedValues;
    private Iterator<Object> valueIterator;

    public ElementLocationRandomIterator(ListVariableStateSupply<Solution_> listVariableStateSupply,
            EntitySelector<Solution_> entitySelector, EntityIndependentValueSelector<Solution_> valueSelector,
            Random workingRandom, long totalSize, boolean allowsUnassignedValues) {
        this.listVariableStateSupply = listVariableStateSupply;
        this.listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
        this.entitySelector = entitySelector;
        this.valueSelector = valueSelector;
        this.entityIterator = entitySelector.iterator();
        this.workingRandom = workingRandom;
        this.totalSize = totalSize;
        if (totalSize < 1) {
            throw new IllegalStateException("Impossible state: totalSize (%d) < 1"
                    .formatted(totalSize));
        }
        this.allowsUnassignedValues = allowsUnassignedValues;
        this.valueIterator = null;
    }

    @Override
    public boolean hasNext() {
        // The valueSelector's hasNext() is insignificant.
        // The next random destination exists if and only if there is a next entity.
        return entityIterator.hasNext();
    }

    @Override
    public ElementLocation next() {
        // This code operates under the assumption that the entity selector already filtered out all immovable entities.
        // At this point, entities are only partially pinned, or not pinned at all.
        var entitySize = entitySelector.getSize();
        var entityBoundary = allowsUnassignedValues ? entitySize + 1 : entitySize;
        long random = RandomUtils.nextLong(workingRandom, totalSize);
        if (allowsUnassignedValues && random == 0) {
            // We have already excluded all unassigned elements,
            // the only way to get an unassigned destination is to explicitly add it.
            return ElementLocation.unassigned();
        } else if (random < entityBoundary) {
            // Start with the first unpinned value of each entity, or zero if no pinning.
            var entity = entityIterator.next();
            return new LocationInList(entity, listVariableDescriptor.getFirstUnpinnedIndex(entity));
        } else { // Value selector already returns only unpinned values.
            if (valueIterator == null) {
                valueIterator = valueSelector.iterator();
            }
            var value = valueIterator.hasNext() ? valueIterator.next() : null;
            if (value == null) {
                // No more values are available; happens with pinning and/or unassigned.
                // This is effectively an off-by-N error where the filtering selectors report incorrect sizes
                // on account of not knowing how many values are going to be filtered out.
                // As a fallback, start picking random unpinned destinations until the iteration stops externally.
                // This skews the selection probability towards entities with fewer unpinned values,
                // but at this point, there is no other thing we could possibly do.
                var entity = entityIterator.next();
                int unpinnedSize = listVariableDescriptor.getUnpinnedSubListSize(entity);
                if (unpinnedSize == 0) { // Only the last destination remains.
                    return new LocationInList(entity, listVariableDescriptor.getListSize(entity));
                } else { // +1 to include the destination after the final element in the list.
                    int randomIndex = workingRandom.nextInt(unpinnedSize + 1);
                    return new LocationInList(entity, listVariableDescriptor.getFirstUnpinnedIndex(entity) + randomIndex);
                }
            } else { // +1 to include the destination after the final element in the list.
                var elementLocation = (LocationInList) listVariableStateSupply.getLocationInList(value);
                return new LocationInList(elementLocation.entity(), elementLocation.index() + 1);
            }
        }
    }
}
