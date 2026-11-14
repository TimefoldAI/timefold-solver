package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class records the relationship between each planning value and all entities that include the related value
 * within its value range.
 * <p>
 * Each instance belongs to one planning variable; for that variable, each entity has exactly one value range,
 * and equal ranges are deduplicated.
 * A value records the ranges that contain it, not the entities or values it reaches.
 * A value reaches an entity if the entity's range contains the value,
 * and it reaches another value if one range contains both.
 * This takes O(V·R) memory for V values and R distinct ranges, instead of O(V²).
 * <p>
 * Values that are in the same ranges form a group, and reach the same entities and values (apart from themselves).
 * The entity and value lists are built on first use, once for each group, and shared by its values;
 * so a value list also contains the value itself.
 *
 * @see FromEntityPropertyValueRangeDescriptor
 */
@NullMarked
public final class ReachableValues<Entity_, Value_> {

    private final ReachableValuesIndex<Entity_, Entity_> entitiesIndex;
    private final ReachableValuesIndex<Value_, ReachableItemValue<Value_>> valuesIndex;
    private final RangeIndex rangeIndex;
    private final Class<?> expectedSupertypeOfValue;
    private final @Nullable ValueRangeSorter<Value_> valueRangeSorter;
    private final boolean acceptsNullValue;
    private final @Nullable Predicate<Entity_> movableEntityFilter;
    private final @Nullable Predicate<Value_> movableValueFilter;
    private @Nullable List<Entity_>[] entityListByGroup;
    private @Nullable List<Entity_>[] movableEntityListByGroup;
    private @Nullable Map<Class<?>, @Nullable List<Entity_>[]> movableEntityListByGroupByClass;
    private @Nullable List<Value_>[] valueListByGroup;
    private @Nullable List<Value_>[] movableValueListByGroup;
    private int @Nullable [] positionInValueList;
    private @Nullable ReachableItemValue<Value_> firstCachedObject;
    private @Nullable ReachableItemValue<Value_> secondCachedObject;
    private @Nullable ReachableItemValue<Value_> repeatedOrigin;
    private int repeatedOriginCount;
    private @Nullable BitSet repeatedOriginValues;

    /**
     * @param movableEntityFilter null if every entity is movable
     * @param movableValueFilter null if every value is movable; only list variable values can be pinned
     */
    public ReachableValues(ReachableValuesIndex<Entity_, Entity_> entitiesIndex,
            ReachableValuesIndex<Value_, ReachableItemValue<Value_>> valuesIndex, RangeIndex rangeIndex,
            Class<?> expectedSupertypeOfValue, @Nullable ValueRangeSorter<Value_> valueRangeSorter, boolean acceptsNullValue,
            @Nullable Predicate<Entity_> movableEntityFilter, @Nullable Predicate<Value_> movableValueFilter) {
        this.entitiesIndex = entitiesIndex;
        this.valuesIndex = valuesIndex;
        this.rangeIndex = rangeIndex;
        this.expectedSupertypeOfValue = expectedSupertypeOfValue;
        this.valueRangeSorter = valueRangeSorter;
        this.acceptsNullValue = acceptsNullValue;
        this.movableEntityFilter = movableEntityFilter;
        this.movableValueFilter = movableValueFilter;
        clear();
    }

    private @Nullable ReachableItemValue<Value_> fetchItemValue(Value_ value) {
        ReachableItemValue<Value_> selected = null;
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

    /**
     * @return random access, unmodifiable, in the order of the entity index, shared with other callers
     */
    public List<Entity_> extractEntitiesAsList(Value_ value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var entityList = entityListByGroup[itemValue.group];
        if (entityList == null) {
            entityList = buildEntityList(itemValue, null);
            entityListByGroup[itemValue.group] = entityList;
        }
        return entityList;
    }

    /**
     * As {@link #extractEntitiesAsList(Object)}, but without the entities that are not movable.
     * The movability is cached and only changes with a problem change, which rebuilds this instance.
     *
     * @return unmodifiable, random access, shared with other callers
     */
    public List<Entity_> extractMovableEntitiesAsList(Value_ value) {
        if (movableEntityFilter == null) {
            return extractEntitiesAsList(value);
        }
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var entityList = movableEntityListByGroup[itemValue.group];
        if (entityList == null) {
            entityList = buildEntityList(itemValue, movableEntityFilter);
            movableEntityListByGroup[itemValue.group] = entityList;
        }
        return entityList;
    }

    /**
     * As {@link #extractMovableEntitiesAsList(Object)}, but only the instances of the given class.
     * Used for a subclass whose variable is inherited and indexed for the whole class hierarchy.
     *
     * @return random access, unmodifiable, shared with other callers
     */
    @SuppressWarnings("unchecked")
    public List<Entity_> extractMovableEntitiesAsList(Value_ value, Class<?> entityClass) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        if (movableEntityListByGroupByClass == null) {
            movableEntityListByGroupByClass = new HashMap<>();
        }
        var entityListArray = movableEntityListByGroupByClass.get(entityClass);
        if (entityListArray == null) {
            entityListArray = new List[rangeIndex.groupCount()];
            movableEntityListByGroupByClass.put(entityClass, entityListArray);
        }
        var entityList = entityListArray[itemValue.group];
        if (entityList == null) {
            // The class check first: it is cheaper, and the movable filter can assume the class.
            Predicate<Entity_> filter = entityClass::isInstance;
            if (movableEntityFilter != null) {
                filter = filter.and(movableEntityFilter);
            }
            entityList = buildEntityList(itemValue, filter);
            entityListArray[itemValue.group] = entityList;
        }
        return entityList;
    }

    @SuppressWarnings("unchecked")
    private List<Entity_> buildEntityList(ReachableItemValue<Value_> itemValue,
            @Nullable Predicate<Entity_> filter) {
        var ranges = itemValue.ranges;
        var allEntities = entitiesIndex.allItems();
        // A bitset restores the entity index order.
        var entityIndices = new BitSet(allEntities.size());
        var size = 0;
        for (var range = ranges.nextSetBit(0); range >= 0; range = ranges.nextSetBit(range + 1)) {
            for (var entityIndex : rangeIndex.entitiesOfRange()[range]) {
                entityIndices.set(entityIndex);
                size++;
            }
        }
        var entityArray = (Entity_[]) new Object[size];
        var entityCount = 0;
        for (var index = entityIndices.nextSetBit(0); index >= 0; index = entityIndices.nextSetBit(index + 1)) {
            var entity = allEntities.get(index);
            if (filter == null || filter.test(entity)) {
                entityArray[entityCount++] = entity;
            }
        }
        return toSharedList(entityArray, entityCount);
    }

    public int getReachableEntitiesSize(Value_ value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return 0;
        }
        return getReachableEntitiesSize(itemValue);
    }

    // Could be cached, but the only caller asks one time per value.
    private int getReachableEntitiesSize(ReachableItemValue<Value_> itemValue) {
        var ranges = itemValue.ranges;
        var size = 0;
        for (var range = ranges.nextSetBit(0); range >= 0; range = ranges.nextSetBit(range + 1)) {
            size += rangeIndex.entitiesOfRange()[range].length;
        }
        return size;
    }

    /**
     * @return the values that share a range with the given value, including the given value;
     *         random access, unmodifiable, sorted if this instance has a sorter, shared with other callers
     */
    public List<Value_> extractValuesAsList(Value_ value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var valueList = valueListByGroup[itemValue.group];
        if (valueList == null) {
            valueList = buildValueList(itemValue, null);
            valueListByGroup[itemValue.group] = valueList;
        }
        return valueList;
    }

    /**
     * @return the index of the value in {@link #extractValuesAsList(Object)} of that value, or -1 if it is not indexed or not
     *         in that list
     */
    public int indexOfInValueList(Value_ value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return -1;
        }
        if (positionInValueList == null) {
            positionInValueList = new int[valuesIndex.allItems().size()];
            // -1 means that the positions of the value's group are not stored yet.
            Arrays.fill(positionInValueList, -1);
        }
        if (positionInValueList[itemValue.ordinal] < 0) {
            storePositions(itemValue.group, extractValuesAsList(value));
        }
        return positionInValueList[itemValue.ordinal];
    }

    // A list also contains values of other groups; their positions belong to their own group's list.
    private void storePositions(int group, List<Value_> valueList) {
        var allItems = valuesIndex.allItems();
        for (var i = 0; i < valueList.size(); i++) {
            var item = allItems.get(valuesIndex.indexMap().get(valueList.get(i)));
            if (item.group == group) {
                positionInValueList[item.ordinal] = i;
            }
        }
    }

    /**
     * As {@link #extractValuesAsList(Object)}, but without the values that are not movable.
     * The movability is cached and only changes with a problem change, which rebuilds this instance.
     *
     * @return unmodifiable, random access, shared with other callers
     */
    public List<Value_> extractMovableValuesAsList(Value_ value) {
        if (movableValueFilter == null) {
            return extractValuesAsList(value);
        }
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        var valueList = movableValueListByGroup[itemValue.group];
        if (valueList == null) {
            valueList = buildValueList(itemValue, movableValueFilter);
            movableValueListByGroup[itemValue.group] = valueList;
        }
        return valueList;
    }

    /**
     * @return the values of all ranges of the given value's group, including the given value
     */
    @SuppressWarnings("unchecked")
    private List<Value_> buildValueList(ReachableItemValue<Value_> itemValue,
            @Nullable Predicate<Value_> filter) {
        var allValues = valuesIndex.allItems();
        var valueIndices = buildValueIndices(itemValue);
        var valueArray = (Value_[]) new Object[valueIndices.cardinality()];
        var valueCount = 0;
        for (var index = valueIndices.nextSetBit(0); index >= 0; index = valueIndices.nextSetBit(index + 1)) {
            var value = allValues.get(index).value;
            if (filter == null || filter.test(value)) {
                valueArray[valueCount++] = value;
            }
        }
        if (valueRangeSorter != null) {
            valueRangeSorter.sort(Arrays.asList(valueArray).subList(0, valueCount));
        }
        return toSharedList(valueArray, valueCount);
    }

    /**
     * @return the indices of the values in all ranges of the given value, including the given value
     */
    private BitSet buildValueIndices(ReachableItemValue<Value_> itemValue) {
        var ranges = itemValue.ranges;
        var valueIndices = new BitSet(valuesIndex.allItems().size());
        for (var range = ranges.nextSetBit(0); range >= 0; range = ranges.nextSetBit(range + 1)) {
            valueIndices.or(rangeIndex.valuesOfRange()[range]);
        }
        return valueIndices;
    }

    public Set<Entity_> extractAllEntitiesAsSet() {
        return entitiesIndex.indexMap().keySet();
    }

    public Set<Value_> extractAllValuesAsSet() {
        return valuesIndex.indexMap().keySet();
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
        return originItemValue.ranges.get(rangeIndex.rangeOfEntity()[entityIndex]);
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
        if (otherValueIndex == null || otherValueIndex == originItemValue.ordinal) {
            return false;
        }
        if (originItemValue != repeatedOrigin) {
            repeatedOrigin = originItemValue;
            repeatedOriginCount = 0;
            repeatedOriginValues = null;
        }
        if (repeatedOriginValues == null && ++repeatedOriginCount >= originItemValue.repeatedOriginThreshold) {
            repeatedOriginValues = buildValueIndices(originItemValue);
        }
        if (repeatedOriginValues != null) {
            // A bit of the origin's own bitset stays in the CPU cache; the other value's ranges would not.
            return repeatedOriginValues.get(otherValueIndex);
        }
        var otherItemValue = valuesIndex.allItems().get(otherValueIndex);
        return originItemValue.group == otherItemValue.group || originItemValue.ranges.intersects(otherItemValue.ranges);
    }

    /**
     * @return unmodifiable; the lists are cached and shared,
     *         so one caller that sorts or sets its list would corrupt it for all.
     *         A filtered list is trimmed one time, so that it does not keep the unfiltered size.
     */
    private static <Type_> List<Type_> toSharedList(Type_[] array, int size) {
        // Do it with as little array copying as possible.
        return Collections.unmodifiableList(Arrays.asList(size == array.length ? array : Arrays.copyOf(array, size)));
    }

    private static int toWordCount(int bitCount) {
        return (bitCount + Long.SIZE - 1) / Long.SIZE;
    }

    public boolean acceptsNullValue() {
        return acceptsNullValue;
    }

    public boolean valueHasMatchingType(Value_ value) {
        return expectedSupertypeOfValue.isAssignableFrom(value.getClass());
    }

    public ReachableValues<Entity_, Value_> copy(ValueRangeSorter<Value_> sorterAdapter) {
        return new ReachableValues<>(entitiesIndex, valuesIndex, rangeIndex, expectedSupertypeOfValue, sorterAdapter,
                acceptsNullValue, movableEntityFilter, movableValueFilter);
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        firstCachedObject = null;
        secondCachedObject = null;
        repeatedOrigin = null;
        repeatedOriginCount = 0;
        repeatedOriginValues = null;
        var groupCount = rangeIndex.groupCount();
        this.entityListByGroup = new List[groupCount];
        this.movableEntityListByGroup = new List[groupCount];
        this.movableEntityListByGroupByClass = null;
        this.valueListByGroup = new List[groupCount];
        this.movableValueListByGroup = new List[groupCount];
        this.positionInValueList = null;
    }

    @NullMarked
    public static final class ReachableItemValue<Value_> {
        private final int ordinal;
        private final Value_ value;
        private final BitSet ranges;
        private int group = -1;
        private int repeatedOriginThreshold;

        public ReachableItemValue(int ordinal, Value_ value, int rangeCount) {
            this.ordinal = ordinal;
            this.value = value;
            this.ranges = new BitSet(rangeCount);
        }

        public void addRange(int rangeIndex) {
            ranges.set(rangeIndex);
        }

        /**
         * @return the indices of the ranges that contain this value; do not modify
         */
        public BitSet getRanges() {
            return ranges;
        }

        /**
         * Nearby distance matrices ask about one origin and many other values in a row.
         * After the repeated origin threshold of calls, the range intersections have cost as much as
         * building the origin's value bitset, so building it keeps the total cost at most twice the cost of the better choice.
         *
         * @param group the same for all values with equal {@link #getRanges()}
         * @param rangeCount the number of distinct ranges
         * @param valueCount the number of values
         */
        public void setGroup(int group, int rangeCount, int valueCount) {
            this.group = group;
            var buildCost = (long) ranges.cardinality() * toWordCount(valueCount);
            this.repeatedOriginThreshold = Math.clamp(buildCost / Math.max(1, toWordCount(rangeCount)), 1, Integer.MAX_VALUE);
        }

    }

    @NullMarked
    public record ReachableValuesIndex<Value_, Type_>(Map<Value_, Integer> indexMap, List<Type_> allItems) {

    }

    /**
     * @param rangeOfEntity for each entity index, the index of its range
     * @param entitiesOfRange for each range index, the sorted indices of the entities with that range
     * @param valuesOfRange for each range index, the indices of the values in that range
     * @param groupCount the number of distinct range sets among the values
     */
    @NullMarked
    public record RangeIndex(int[] rangeOfEntity, int[][] entitiesOfRange, BitSet[] valuesOfRange, int groupCount) {

        @Override
        public boolean equals(Object other) {
            return other instanceof RangeIndex that
                    && groupCount == that.groupCount
                    && Arrays.equals(rangeOfEntity, that.rangeOfEntity)
                    && Arrays.deepEquals(entitiesOfRange, that.entitiesOfRange)
                    && Arrays.equals(valuesOfRange, that.valuesOfRange);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(rangeOfEntity), Arrays.deepHashCode(entitiesOfRange),
                    Arrays.hashCode(valuesOfRange), groupCount);
        }

        @Override
        public String toString() {
            return "RangeIndex[entityCount=%d, rangeCount=%d, groupCount=%d]".formatted(rangeOfEntity.length,
                    entitiesOfRange.length, groupCount);
        }

    }

}
