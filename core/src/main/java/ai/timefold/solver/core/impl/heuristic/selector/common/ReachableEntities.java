package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntityByEntitySelector;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;

import org.jspecify.annotations.NullMarked;

/**
 * This class records the relationship between each entity and all its reachable entities.
 *
 * @see FilteringEntityByEntitySelector
 */
public class ReachableEntities<Solution_> {

    private final EntityDescriptor<Solution_> entityDescriptor;
    private final Map<Object, Integer> entitiesIndex;
    private final List<Object> allEntities;
    private final ReachableItem[] items;
    private final ValueRangeManager<Solution_> valueRangeManager;

    private Object cachedEntity = null;
    private int cachedOrdinal = -1;

    public ReachableEntities(EntityDescriptor<Solution_> entityDescriptor, List<Object> allEntities,
            ValueRangeManager<Solution_> valueRangeManager) {
        this.entityDescriptor = entityDescriptor;
        this.entitiesIndex = new IdentityHashMap<>(allEntities.size());
        for (var i = 0; i < allEntities.size(); i++) {
            entitiesIndex.put(allEntities.get(i), i);
        }
        this.allEntities = allEntities;
        this.items = new ReachableItem[entitiesIndex.size()];
        this.valueRangeManager = valueRangeManager;
    }

    public int getReachableEntityOrdinal(Object entity) {
        if (!Objects.requireNonNull(entity).getClass().isAssignableFrom(entityDescriptor.getEntityClass())) {
            throw new IllegalArgumentException(
                    "Impossible state: the entity class %s does not match with the expected class %s."
                            .formatted(entity.getClass(), entityDescriptor.getEntityClass()));
        }
        if (cachedOrdinal == -1 || cachedEntity != entity) {
            var ordinal = entitiesIndex.get(entity);
            if (ordinal == null) {
                throw new IllegalArgumentException("Impossible state: the entity %s is not indexed.".formatted(entity));
            }
            this.cachedOrdinal = ordinal;
            this.cachedEntity = entity;
        }
        return cachedOrdinal;
    }

    public Object getReachableEntity(int ordinal) {
        ensureLoaded(ordinal);
        return items[ordinal].entity();
    }

    public boolean isReachable(int entityOrdinal, int otherEntityOrdinal) {
        ensureLoaded(entityOrdinal);
        return items[entityOrdinal].contains(otherEntityOrdinal);
    }

    public Iterator<Integer> randomIterator(int entityOrdinal, Random workingRandom) {
        ensureLoaded(entityOrdinal);
        return new RandomEntityIterator(items[entityOrdinal], workingRandom);
    }

    public ListIterator<Integer> listIterator(int entityOrdinal, int index) {
        ensureLoaded(entityOrdinal);
        return new ListEntityIterator(items[entityOrdinal], index);
    }

    private void ensureLoaded(int ordinal) {
        if (items[ordinal] != null) {
            return;
        }
        items[ordinal] = new ReachableItem(ordinal, allEntities.get(ordinal), new BitSet(allEntities.size()));
        var descriptorList = entityDescriptor.getGenuineVariableDescriptorList()
                .stream()
                .filter(descriptor -> !descriptor.canExtractValueRangeFromSolution())
                .toList();
        for (var i = 0; i < allEntities.size(); i++) {
            if (i == ordinal) {
                continue;
            }
            for (var descriptor : descriptorList) {
                var reachableValues = valueRangeManager.getReachableValues(descriptor);
                if (reachableValues.isEntityToEntityReachable(ordinal, i)) {
                    items[ordinal].addReachableEntity(i);
                }
            }
        }
    }

    @NullMarked
    private record ReachableItem(int ordinal, Object entity, BitSet reachableEntities) {

        public void addReachableEntity(int ordinal) {
            reachableEntities.set(ordinal);
        }

        boolean contains(int entityOrdinal) {
            return reachableEntities.get(entityOrdinal);
        }
    }

    private static class RandomEntityIterator implements Iterator<Integer> {
        private final Integer[] allReachableEntities;
        private final Random workingRandom;

        private RandomEntityIterator(ReachableItem entity, Random workingRandom) {
            this.allReachableEntities = entity.reachableEntities.stream().boxed().toArray(Integer[]::new);
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return allReachableEntities.length > 0;
        }

        @Override
        public Integer next() {
            return allReachableEntities[workingRandom.nextInt(allReachableEntities.length)];
        }
    }

    private static class ListEntityIterator implements ListIterator<Integer> {

        private final Integer[] allReachableEntities;
        private final int minIndex;
        private int index = 0;

        ListEntityIterator(ReachableItem entity, int minIndex) {
            this.allReachableEntities = entity.reachableEntities.stream().boxed().toArray(Integer[]::new);
            this.minIndex = minIndex;
            this.index = minIndex;
        }

        @Override
        public boolean hasNext() {
            return index < allReachableEntities.length;
        }

        @Override
        public Integer next() {
            if (index >= allReachableEntities.length) {
                throw new NoSuchElementException();
            }
            return allReachableEntities[index++];
        }

        @Override
        public boolean hasPrevious() {
            return index - 1 > minIndex;
        }

        @Override
        public Integer previous() {
            if (index == allReachableEntities.length) {
                index -= 2;
            }
            if (index < minIndex) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return allReachableEntities[index--];
        }

        @Override
        public int nextIndex() {
            return Math.max(allReachableEntities.length - 1, index + 1);
        }

        @Override
        public int previousIndex() {
            return Math.max(minIndex, index - 1);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Integer value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Integer value) {
            throw new UnsupportedOperationException();
        }
    }
}
