package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class records the relationship between each planning value and all entities that include the related value
 * within its value range.
 *
 * @see FromEntityPropertyValueRangeDescriptor
 */
@NullMarked
public final class ReachableValues {

    private final @Nullable Class<?> valueClass;
    private final boolean acceptsNullValue;
    private final Map<Object, Integer> valuesIndex;
    private final ReachableItemValue[] values;
    private final Map<Object, Integer> entitiesIndex;
    private final ReachableItemEntity[] entities;
    private int cachedEntityOrdinal = -1;
    private @Nullable Object cachedEntity = null;
    private @Nullable ReachableItemValue cachedValue = null;

    public ReachableValues(@Nullable Class<?> valueClass, boolean acceptsNullValue, Map<Object, Integer> valuesIndex,
            ReachableItemValue[] values, Map<Object, Integer> entitiesIndex, ReachableItemEntity[] entities) {
        this.valueClass = valueClass;
        this.acceptsNullValue = acceptsNullValue;
        this.valuesIndex = valuesIndex;
        this.values = values;
        this.entitiesIndex = entitiesIndex;
        this.entities = entities;
    }

    public int getValueOrdinal(Object value) {
        if (cachedValue == null || cachedValue.value() != value) {
            var index = valuesIndex.get(value);
            if (index == null) {
                return -1;
            }
            cachedValue = values[index];
        }
        return cachedValue.ordinal();
    }

    public int getEntityOrdinal(Object entity) {
        if (cachedEntityOrdinal == -1 || cachedEntity != entity) {
            this.cachedEntity = entity;
            this.cachedEntityOrdinal = entitiesIndex.get(entity);
        }
        return cachedEntityOrdinal;
    }

    public Object getEntity(int ordinal) {
        return entities[ordinal].entity();
    }

    public Object getValue(int ordinal) {
        return values[ordinal].value();
    }

    public Iterator<Integer> getOriginalEntityIterator(int valueOrdinal) {
        var reachableEntities = values[valueOrdinal].reachableEntities.stream().boxed().toArray(Integer[]::new);
        return new OriginalIterator(reachableEntities);
    }

    public Iterator<Integer> getRandomEntityIterator(int valueOrdinal, Random workingRandom) {
        var reachableEntities = values[valueOrdinal].reachableEntities.stream().boxed().toArray(Integer[]::new);
        return new RandomIterator(reachableEntities, workingRandom);
    }

    public Iterator<Integer> getOriginalValueIterator(int valueOrdinal) {
        var reachableEntities = values[valueOrdinal].reachableValues.stream().boxed().toArray(Integer[]::new);
        return new OriginalIterator(reachableEntities);
    }

    public Iterator<Integer> getRandomValueIterator(int valueOrdinal, Random workingRandom) {
        var reachableEntities = values[valueOrdinal].reachableValues.stream().boxed().toArray(Integer[]::new);
        return new RandomIterator(reachableEntities, workingRandom);
    }

    public int getSize() {
        return values.length;
    }

    public boolean isEntityReachable(int valueOrdinal, @Nullable Integer entityOrdinal) {
        if (entityOrdinal == null || entityOrdinal == -1) {
            return true;
        }
        return values[valueOrdinal].reachableEntities.get(entityOrdinal);
    }

    public boolean isValueReachable(int valueOrdinal, @Nullable Integer otherValueOrdinal) {
        if (otherValueOrdinal == null || otherValueOrdinal == -1) {
            return acceptsNullValue;
        }
        return values[valueOrdinal].reachableValues.get(otherValueOrdinal);
    }

    public boolean isEntityToEntityReachable(int entityOrdinal, int otherEntityOrdinal) {
        return entities[entityOrdinal].reachableValues.intersects(entities[otherEntityOrdinal].reachableValues());
    }

    public boolean entityContains(int entityOrdinal, @Nullable Integer valueOrdinal) {
        if (valueOrdinal == null || valueOrdinal == -1) {
            return false;
        }
        return entities[entityOrdinal].reachableValues().get(valueOrdinal);
    }

    public boolean matchesValueClass(Object value) {
        return valueClass.isAssignableFrom(Objects.requireNonNull(value).getClass());
    }

    @NullMarked
    public record ReachableItemValue(int ordinal, Object value, BitSet reachableEntities, BitSet reachableValues) {

        public void addEntity(int entityOrdinal) {
            reachableEntities.set(entityOrdinal);
        }

        public void addValue(int valueOrdinal) {
            reachableValues.set(valueOrdinal);
        }
    }

    @NullMarked
    public record ReachableItemEntity(int ordinal, Object entity, BitSet reachableValues) {

        public void addValue(int valueOrdinal) {
            reachableValues.set(valueOrdinal);
        }
    }

    private static class OriginalIterator implements Iterator<Integer> {

        private final Integer[] reachableOrdinalValues;
        private int index;

        OriginalIterator(Integer[] reachableOrdinalValues) {
            this.reachableOrdinalValues = reachableOrdinalValues;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < reachableOrdinalValues.length;
        }

        @Override
        public Integer next() {
            if (index >= reachableOrdinalValues.length) {
                throw new NoSuchElementException();
            }
            return reachableOrdinalValues[index++];
        }
    }

    private static class RandomIterator implements Iterator<Integer> {
        private final Integer[] reachableOrdinalValues;
        private final Random workingRandom;

        private RandomIterator(Integer[] allReachableEntitiese, Random workingRandom) {
            this.reachableOrdinalValues = allReachableEntitiese;
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return reachableOrdinalValues.length > 0;
        }

        @Override
        public Integer next() {
            return reachableOrdinalValues[workingRandom.nextInt(reachableOrdinalValues.length)];
        }
    }
}
