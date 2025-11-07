package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.AbstractList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

    private final Map<Object, Integer> entitiesIndex;
    private final List<Object> allEntities;
    private final Map<Object, Integer> valuesIndex;
    private final List<ReachableItemValue> allValues;
    private final @Nullable Class<?> valueClass;
    private final boolean acceptsNullValue;
    private @Nullable ReachableItemValue firstCachedObject;
    private @Nullable ReachableItemValue secondCachedObject;

    public ReachableValues(Map<Object, Integer> entityIndexMap, List<Object> entityList, Map<Object, Integer> valueIndexMap,
            List<ReachableItemValue> reachableValueList, @Nullable Class<?> valueClass, boolean acceptsNullValue) {
        this.entitiesIndex = entityIndexMap;
        this.allEntities = entityList;
        this.valuesIndex = valueIndexMap;
        this.allValues = reachableValueList;
        this.valueClass = valueClass;
        this.acceptsNullValue = acceptsNullValue;
    }

    private @Nullable ReachableItemValue fetchItemValue(Object value) {
        ReachableItemValue selected = null;
        if (firstCachedObject != null && firstCachedObject.value == value) {
            selected = firstCachedObject;
        } else if (secondCachedObject != null && secondCachedObject.value == value) {
            selected = secondCachedObject;
            // The most recently used item is moved to the first position.
            // The goal is to try to keep recently used items in the cache.
            secondCachedObject = firstCachedObject;
            firstCachedObject = selected;
        }
        if (selected == null) {
            var index = valuesIndex.get(value);
            if (index == null) {
                return null;
            }
            selected = allValues.get(index);
            secondCachedObject = firstCachedObject;
            firstCachedObject = selected;
        }
        return selected;
    }

    public List<Object> extractEntitiesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        return itemValue.getRandomAccessEntityList(allEntities);
    }

    public List<Object> extractValuesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        return itemValue.getRandomAccessValueList(allValues);
    }

    public int getSize() {
        return allValues.size();
    }

    public boolean isEntityReachable(@Nullable Object origin, @Nullable Object entity) {
        if (entity == null) {
            return true;
        }
        if (origin == null) {
            return acceptsNullValue;
        }
        var originItemValue = fetchItemValue(origin);
        if (originItemValue == null) {
            return false;
        }
        var entityIndex = entitiesIndex.get(entity);
        if (entityIndex == null) {
            throw new IllegalStateException("The entity %s is not indexed.".formatted(entity));
        }
        return originItemValue.containsEntity(entityIndex);
    }

    public boolean isValueReachable(Object origin, @Nullable Object otherValue) {
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        if (otherValue == null) {
            return acceptsNullValue;
        }
        var otherValueIndex = valuesIndex.get(Objects.requireNonNull(otherValue));
        if (otherValueIndex == null) {
            return false;
        }
        return originItemValue.containsValue(otherValueIndex);
    }

    public boolean acceptsNullValue() {
        return acceptsNullValue;
    }

    public boolean matchesValueClass(Object value) {
        return valueClass != null && valueClass.isAssignableFrom(Objects.requireNonNull(value).getClass());
    }

    @NullMarked
    public static final class ReachableItemValue {
        private final Object value;
        private final BitSet entityBitSet;
        private final BitSet valueBitSet;
        // The entity and value list are calculated only when needed.
        // The goal is to avoid loading unused data upfront, as it may affect scalability.
        private @Nullable List<Object> onDemandRandomAccessEntityList;
        private @Nullable List<Object> onDemandRandomAccessValueList;

        public ReachableItemValue(Object value, int entityListSize, int valueListSize) {
            this.value = value;
            this.entityBitSet = new BitSet(entityListSize);
            this.valueBitSet = new BitSet(valueListSize);
        }

        public void addEntity(int entityIndex) {
            entityBitSet.set(entityIndex);
        }

        public void addValue(int valueIndex) {
            valueBitSet.set(valueIndex);
        }

        boolean containsEntity(int entityIndex) {
            return entityBitSet.get(entityIndex);
        }

        boolean containsValue(int valueIndex) {
            return valueBitSet.get(valueIndex);
        }

        private static int[] extractAllIndexes(BitSet bitSet) {
            var indexes = new int[bitSet.cardinality()];
            var idx = 0;
            for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
                indexes[idx++] = i;
            }
            return indexes;
        }

        List<Object> getRandomAccessEntityList(List<Object> allEntities) {
            if (onDemandRandomAccessEntityList == null) {
                onDemandRandomAccessEntityList = new ArrayIndexedList<>(extractAllIndexes(entityBitSet), allEntities, null);
            }
            return onDemandRandomAccessEntityList;
        }

        List<Object> getRandomAccessValueList(List<ReachableItemValue> allValues) {
            if (onDemandRandomAccessValueList == null) {
                onDemandRandomAccessValueList = new ArrayIndexedList<>(extractAllIndexes(valueBitSet), allValues, v -> v.value);
            }
            return onDemandRandomAccessValueList;
        }
    }

    @NullMarked
    private static final class ArrayIndexedList<T, V> extends AbstractList<V> {

        private final int[] valueIndex;
        private final List<T> allValues;
        private final @Nullable Function<T, V> valueExtractor;

        private ArrayIndexedList(int[] valueIndex, List<T> allValues, @Nullable Function<T, V> valueExtractor) {
            this.valueIndex = valueIndex;
            this.allValues = allValues;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public V get(int index) {
            if (index < 0 || index >= valueIndex.length) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            var value = allValues.get(valueIndex[index]);
            if (valueExtractor == null) {
                return (V) value;
            } else {
                return valueExtractor.apply(value);
            }
        }

        @Override
        public int size() {
            return valueIndex.length;
        }
    }

}
