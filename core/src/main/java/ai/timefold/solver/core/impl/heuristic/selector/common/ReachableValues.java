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
public final class ReachableValues<Entity_, Value_> {

    private final ReachableValuesIndex<Entity_, Entity_> entitiesIndex;
    private final ReachableValuesIndex<Value_, ReachableItemValue<Entity_, Value_>> valuesIndex;
    private final Class<?> expectedSupertypeOfValue;
    private final @Nullable ValueRangeSorter<Value_> valueRangeSorter;
    private final boolean acceptsNullValue;
    private @Nullable List<Entity_>[] onDemandRandomAccessEntity;
    private @Nullable List<Value_>[] onDemandRandomAccessValue;
    private @Nullable ReachableItemValue<Entity_, Value_> firstCachedObject;
    private @Nullable ReachableItemValue<Entity_, Value_> secondCachedObject;

    public ReachableValues(ReachableValuesIndex<Entity_, Entity_> entitiesIndex,
            ReachableValuesIndex<Value_, ReachableItemValue<Entity_, Value_>> valuesIndex, Class<?> expectedSupertypeOfValue,
            @Nullable ValueRangeSorter<Value_> valueRangeSorter, boolean acceptsNullValue) {
        this.entitiesIndex = entitiesIndex;
        this.valuesIndex = valuesIndex;
        this.expectedSupertypeOfValue = expectedSupertypeOfValue;
        this.valueRangeSorter = valueRangeSorter;
        this.acceptsNullValue = acceptsNullValue;
        this.onDemandRandomAccessEntity = new List[valuesIndex.allItems().size()];
        this.onDemandRandomAccessValue = new List[valuesIndex.allItems().size()];
    }

    private @Nullable ReachableItemValue<Entity_, Value_> fetchItemValue(Object value) {
        ReachableItemValue<Entity_, Value_> selected = null;
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
            var index = valuesIndex.indexMap().get(value);
            if (index == null) {
                return null;
            }
            selected = valuesIndex.allItems().get(index);
            secondCachedObject = firstCachedObject;
            firstCachedObject = selected;
        }
        return selected;
    }

    public List<Entity_> extractEntitiesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var entityList = onDemandRandomAccessEntity[itemValue.ordinal];
        if (entityList == null) {
            entityList = itemValue.getRandomAccessEntityList(entitiesIndex.allItems());
            onDemandRandomAccessEntity[itemValue.ordinal] = entityList;
        }
        return entityList;
    }

    public List<Value_> extractValuesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var valueList = onDemandRandomAccessValue[itemValue.ordinal];
        if (valueList == null) {
            valueList = itemValue.getRandomAccessValueList(valuesIndex.allItems(), valueRangeSorter);
            onDemandRandomAccessValue[itemValue.ordinal] = valueList;
        }
        return valueList;
    }

    public int getSize() {
        return valuesIndex.allItems().size();
    }

    public boolean isEntityReachable(@Nullable Value_ origin, @Nullable Entity_ entity) {
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
        var entityIndex = entitiesIndex.indexMap().get(entity);
        if (entityIndex == null) {
            throw new IllegalStateException("The entity %s is not indexed.".formatted(entity));
        }
        return originItemValue.containsEntity(entityIndex);
    }

    public boolean isValueReachable(Value_ origin, @Nullable Value_ otherValue) {
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        if (otherValue == null) {
            return acceptsNullValue;
        }
        var otherValueIndex = valuesIndex.indexMap().get(Objects.requireNonNull(otherValue));
        if (otherValueIndex == null) {
            return false;
        }
        return originItemValue.containsValue(otherValueIndex);
    }

    public boolean acceptsNullValue() {
        return acceptsNullValue;
    }

    public boolean valueHasMatchingType(Value_ value) {
        return expectedSupertypeOfValue.isAssignableFrom(value.getClass());
    }

    public ReachableValues<Entity_, Value_> copy(ValueRangeSorter<Value_> sorterAdapter) {
        return new ReachableValues<>(entitiesIndex, valuesIndex, expectedSupertypeOfValue, sorterAdapter, acceptsNullValue);
    }

    public void clear() {
        firstCachedObject = null;
        secondCachedObject = null;
        this.onDemandRandomAccessEntity = new List[valuesIndex.allItems().size()];
        this.onDemandRandomAccessValue = new List[valuesIndex.allItems().size()];
    }

    @NullMarked
    public static final class ReachableItemValue<Entity_, Value_> {
        private final int ordinal;
        private final Value_ value;
        private final BitSet entityBitSet;
        private final BitSet valueBitSet;

        public ReachableItemValue(int ordinal, Value_ value, int entityListSize, int valueListSize) {
            this.ordinal = ordinal;
            this.value = value;
            this.entityBitSet = new BitSet(entityListSize);
            this.valueBitSet = new BitSet(valueListSize);
        }

        public void addEntity(int entityIndex) {
            entityBitSet.set(entityIndex);
        }

        public void addValuesExcept(BitSet values, int exceptValueIndex) {
            valueBitSet.or(values);
            valueBitSet.clear(exceptValueIndex);
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

        List<Entity_> getRandomAccessEntityList(List<Entity_> allEntities) {
            return new ArrayIndexedList<>(extractAllIndexes(entityBitSet), allEntities, null);
        }

        List<Value_> getRandomAccessValueList(List<ReachableItemValue<Entity_, Value_>> allValues,
                @Nullable ValueRangeSorter<Value_> valueRangeSorter) {
            var valuesList = new ArrayIndexedList<>(extractAllIndexes(valueBitSet), allValues,
                    v -> (Value_) v.value);
            if (valueRangeSorter != null) {
                valueRangeSorter.sort(valuesList);
            }
            return valuesList;
        }
    }

    @NullMarked
    public record ReachableValuesIndex<Value_, Type_>(Map<Value_, Integer> indexMap, List<Type_> allItems) {

    }

    @NullMarked
    private static final class ArrayIndexedList<Type_, Value_> extends AbstractList<Value_> {

        private final List<Integer> valueIndex;
        private final List<Type_> allValues;
        private final @Nullable Function<Type_, Value_> valueExtractor;

        private ArrayIndexedList(List<Integer> valueIndex, List<Type_> allValues,
                @Nullable Function<Type_, Value_> valueExtractor) {
            this.valueIndex = valueIndex;
            this.allValues = allValues;
            this.valueExtractor = valueExtractor;
        }

        @Override
        public Value_ get(int index) {
            if (index < 0 || index >= valueIndex.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return getInnerValue(valueIndex.get(index));
        }

        private Value_ getInnerValue(int index) {
            if (index < 0 || index >= allValues.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            var value = allValues.get(index);
            if (valueExtractor == null) {
                return (Value_) value;
            } else {
                return valueExtractor.apply(value);
            }
        }

        @Override
        public void sort(@Nullable Comparator<? super Value_> comparator) {
            if (comparator == null) {
                return;
            }
            valueIndex.sort((Integer v1, Integer v2) -> comparator.compare(getInnerValue(v1), getInnerValue(v2)));
        }

        @Override
        public int size() {
            return valueIndex.size();
        }
    }

}
