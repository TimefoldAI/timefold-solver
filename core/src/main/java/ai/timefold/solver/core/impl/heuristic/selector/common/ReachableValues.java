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
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.ValueRangeManager;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * This class records the relationship between each planning value and all entities that include the related value
 * within its value range.
 *
 * @see FromEntityPropertyValueRangeDescriptor
 */
@NullMarked
public final class ReachableValues<Solution_> {

    private final GenuineVariableDescriptor<Solution_> variableDescriptor;
    private final Map<Object, ReachableItemValue> values;
    private final @Nullable Class<?> valueClass;
    private final int valuesSize;
    private final List<Object> allEntities;
    private final ValueRangeManager<Solution_> valueRangeManager;
    private final boolean acceptsNullValue;
    private @Nullable ReachableItemValue firstCachedObject;
    private @Nullable ReachableItemValue secondCachedObject;

    public ReachableValues(GenuineVariableDescriptor<Solution_> variableDescriptor,
            Map<Object, ReachableItemValue> values, Class<?> valueClass, int valuesSize, List<Object> allEntities,
            ValueRangeManager<Solution_> valueRangeManager, boolean acceptsNullValue) {
        this.variableDescriptor = variableDescriptor;
        this.values = values;
        this.valueClass = valueClass;
        this.valuesSize = valuesSize;
        this.allEntities = allEntities;
        this.valueRangeManager = valueRangeManager;
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
            if (selected == null) {
                // We need to load the values
                selected = loadReachableValue(value);
            }
            secondCachedObject = firstCachedObject;
            firstCachedObject = selected;
        }
        return selected;
    }

    private ReachableItemValue loadReachableValue(Object value) {
        var item = initReachableMap(values, value, allEntities.size(), valuesSize);
        for (var entity : allEntities) {
            var range = valueRangeManager.getFromEntity(variableDescriptor.getValueRangeDescriptor(), entity);
            if (!range.contains(value)) {
                continue;
            }
            item.addEntity(entity);
            for (var i = 0; i < range.getSize(); i++) {
                var otherValue = range.get(i);
                if (otherValue == null || Objects.equals(otherValue, value)) {
                    continue;
                }
                item.addValue(otherValue);
            }
        }
        return item;
    }

    private static ReachableItemValue initReachableMap(Map<Object, ReachableItemValue> reachableValuesMap, Object value,
            int entityListSize, int valueListSize) {
        var item = reachableValuesMap.get(value);
        if (item == null) {
            item = new ReachableItemValue(value, entityListSize, valueListSize);
            reachableValuesMap.put(value, item);
        }
        return item;
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
        return valuesSize;
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
        return originItemValue.entitySet.containsKey(entity);
    }

    public boolean isValueReachable(Object origin, @Nullable Object otherValue) {
        var originItemValue = fetchItemValue(Objects.requireNonNull(origin));
        if (originItemValue == null) {
            return false;
        }
        if (otherValue == null) {
            return acceptsNullValue;
        }
        return originItemValue.valueSet.containsKey(Objects.requireNonNull(otherValue));
    }

    public boolean matchesValueClass(Object value) {
        return valueClass != null && valueClass.isAssignableFrom(Objects.requireNonNull(value).getClass());
    }

    @NullMarked
    public static final class ReachableItemValue {
        private final Object value;
        private final Map<Object, Object> entitySet;
        private final Map<Object, Object> valueSet;
        private final List<Object> randomAccessEntityList;
        private final List<Object> randomAccessValueList;

        public ReachableItemValue(Object value, int entityListSize, int valueListSize) {
            this.value = value;
            this.entitySet = new IdentityHashMap<>(entityListSize);
            this.randomAccessEntityList = new ArrayList<>(entityListSize);
            this.valueSet = ConfigUtils.isGenericTypeImmutable(value.getClass()) ? new LinkedHashMap<>(valueListSize)
                    : new IdentityHashMap<>(valueListSize);
            this.randomAccessValueList = new ArrayList<>(valueListSize);
        }

        public void addEntity(Object entity) {
            if (entitySet.put(entity, entity) == null) {
                randomAccessEntityList.add(entity);
            }
        }

        public void addValue(Object value) {
            if (valueSet.put(value, value) == null) {
                randomAccessValueList.add(value);
            }
        }
    }

}
