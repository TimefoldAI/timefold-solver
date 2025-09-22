package ai.timefold.solver.core.impl.heuristic.selector.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.solver.core.config.util.ConfigUtils;
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

    private final Map<Object, ReachableItemValue> values;
    private final @Nullable Class<?> valueClass;
    private final boolean acceptsNullValue;
    private @Nullable ReachableItemValue firstCachedObject;
    private @Nullable ReachableItemValue secondCachedObject;

    public ReachableValues(Map<Object, ReachableItemValue> values, Class<?> valueClass, boolean acceptsNullValue) {
        this.values = values;
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
            selected = values.get(value);
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
        return itemValue.randomAccessEntityList;
    }

    public List<Object> extractValuesAsList(Object value) {
        var itemValue = fetchItemValue(value);
        if (itemValue == null) {
            return Collections.emptyList();
        }
        return itemValue.randomAccessValueList;
    }

    public int getSize() {
        return values.size();
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
        return originItemValue.entityMap.containsKey(entity);
    }

    public boolean isValueReachable(Object origin, @Nullable Object otherValue) {
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        if (otherValue == null) {
            return acceptsNullValue;
        }
        return originItemValue.valueMap.containsKey(Objects.requireNonNull(otherValue));
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
        private final Map<Object, Object> entityMap;
        private final Map<Object, Object> valueMap;
        private final List<Object> randomAccessEntityList;
        private final List<Object> randomAccessValueList;

        public ReachableItemValue(Object value, int entityListSize, int valueListSize) {
            this.value = value;
            this.entityMap = new IdentityHashMap<>(entityListSize);
            this.randomAccessEntityList = new ArrayList<>(entityListSize);
            this.valueMap = ConfigUtils.isGenericTypeImmutable(value.getClass()) ? new LinkedHashMap<>(valueListSize)
                    : new IdentityHashMap<>(valueListSize);
            this.randomAccessValueList = new ArrayList<>(valueListSize);
        }

        public void addEntity(Object entity) {
            if (entityMap.put(entity, entity) == null) {
                randomAccessEntityList.add(entity);
            }
        }

        public void addValue(Object value) {
            if (valueMap.put(value, value) == null) {
                randomAccessValueList.add(value);
            }
        }
    }

}
