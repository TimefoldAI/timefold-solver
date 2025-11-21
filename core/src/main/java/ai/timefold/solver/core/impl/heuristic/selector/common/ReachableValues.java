package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

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
    private final @Nullable ValueRangeSorter<?> valueRangeSorter;
    private final boolean acceptsNullValue;
    private @Nullable List<Object>[] onDemandRandomAccessEntity;
    private @Nullable List<Object>[] onDemandRandomAccessValue;
    private @Nullable ReachableItemValue firstCachedObject;
    private @Nullable ReachableItemValue secondCachedObject;

    public ReachableValues(Map<Object, Integer> entityIndexMap, List<Object> entityList, Map<Object, Integer> valueIndexMap,
            List<ReachableItemValue> reachableValueList, @Nullable Class<?> valueClass,
            @Nullable ValueRangeSorter<?> valueRangeSorter, boolean acceptsNullValue) {
        this.entitiesIndex = entityIndexMap;
        this.allEntities = entityList;
        this.valuesIndex = valueIndexMap;
        this.allValues = reachableValueList;
        this.valueClass = valueClass;
        this.valueRangeSorter = valueRangeSorter;
        this.acceptsNullValue = acceptsNullValue;
        this.onDemandRandomAccessEntity = new List[allValues.size()];
        this.onDemandRandomAccessValue = new List[allValues.size()];
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
        var entityList = onDemandRandomAccessEntity[itemValue.ordinal];
        if (entityList == null) {
            entityList = itemValue.getRandomAccessEntityList(allEntities);
            onDemandRandomAccessEntity[itemValue.ordinal] = entityList;
        }
        return entityList;
    }

    public List<Object> extractValuesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var valueList = onDemandRandomAccessValue[itemValue.ordinal];
        if (valueList == null) {
            valueList = itemValue.getRandomAccessValueList(allValues, valueRangeSorter);
            onDemandRandomAccessValue[itemValue.ordinal] = valueList;
        }
        return valueList;
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

    public ReachableValues copy(ValueRangeSorter<?> sorterAdapter, boolean deepCopy) {
        var newAllValues = allValues;
        if (deepCopy) {
            newAllValues = new ArrayList<>(allValues.size());
            for (var value : allValues) {
                newAllValues.add(new ReachableItemValue(value.ordinal, value.value, value.entityBitSet, value.valueBitSet));
            }
        }
        return new ReachableValues(entitiesIndex, allEntities, valuesIndex, newAllValues, valueClass, sorterAdapter,
                acceptsNullValue);
    }

    public void clear() {
        firstCachedObject = null;
        secondCachedObject = null;
        this.onDemandRandomAccessEntity = new List[allValues.size()];
        this.onDemandRandomAccessValue = new List[allValues.size()];
    }

    @NullMarked
    public static final class ReachableItemValue {
        private final int ordinal;
        private final Object value;
        private final BitSet entityBitSet;
        private final BitSet valueBitSet;
        private boolean sorted = false;

        public ReachableItemValue(int ordinal, Object value, int entityListSize, int valueListSize) {
            this.ordinal = ordinal;
            this.value = value;
            this.entityBitSet = new BitSet(entityListSize);
            this.valueBitSet = new BitSet(valueListSize);
        }

        private ReachableItemValue(int ordinal, Object value, BitSet entityBitSet, BitSet valueBitSet) {
            this.ordinal = ordinal;
            this.value = value;
            this.entityBitSet = entityBitSet;
            this.valueBitSet = valueBitSet;
        }

        public void addEntity(int entityIndex) {
            entityBitSet.set(entityIndex);
        }

        public void addValues(BitSet values) {
            valueBitSet.or(values);
        }

        boolean containsEntity(int entityIndex) {
            return entityBitSet.get(entityIndex);
        }

        boolean containsValue(int valueIndex) {
            return valueBitSet.get(valueIndex);
        }

        private static List<Integer> extractAllIndexes(BitSet bitSet) {
            var indexes = new ArrayList<Integer>(bitSet.cardinality());
            for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
                indexes.add(i);
            }
            return indexes;
        }

        List<Object> getRandomAccessEntityList(List<Object> allEntities) {
            return new ArrayIndexedList<>(extractAllIndexes(entityBitSet), allEntities, null);
        }

        <V> List<Object> getRandomAccessValueList(List<ReachableItemValue> allValues,
                @Nullable ValueRangeSorter<V> valueRangeSorter) {
            var valuesList = new ArrayIndexedList<>(extractAllIndexes(valueBitSet), allValues,
                    v -> (V) v.value);
            if (valueRangeSorter != null && !sorted) {
                valueRangeSorter.sort(valuesList);
                sorted = true;
            }
            return (List<Object>) valuesList;
        }
    }

    @NullMarked
    private static final class ArrayIndexedList<T, V> extends AbstractList<V> {

        private final List<Integer> valueIndex;
        private final List<T> allValues;
        private final @Nullable Function<T, V> valueExtractor;

        private ArrayIndexedList(List<Integer> valueIndex, List<T> allValues, @Nullable Function<T, V> valueExtractor) {
            this.valueIndex = valueIndex;
            this.allValues = allValues;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public V get(int index) {
            if (index < 0 || index >= valueIndex.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return getInnerValue(valueIndex.get(index));
        }

        private V getInnerValue(int index) {
            if (index < 0 || index >= allValues.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            var value = allValues.get(index);
            if (valueExtractor == null) {
                return (V) value;
            } else {
                return valueExtractor.apply(value);
            }
        }

        @Override
        public void sort(Comparator<? super V> comparator) {
            valueIndex.sort((Integer v1, Integer v2) -> comparator.compare(getInnerValue(v1), getInnerValue(v2)));
        }

        @Override
        public int size() {
            return valueIndex.size();
        }
    }

}
