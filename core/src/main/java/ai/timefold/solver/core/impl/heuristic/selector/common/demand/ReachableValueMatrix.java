package ai.timefold.solver.core.impl.heuristic.selector.common.demand;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
public final class ReachableValueMatrix {

    private final Map<Object, Set<Object>> valueToEntityMap;
    private final Map<Object, Set<Object>> valueToValueMap;
    private final Map<Object, List<Object>> randomAccessValueToEntityMap;
    private final Map<Object, List<Object>> randomAccessValueToValueMap;
    private final @Nullable Class<?> valueClass;

    public ReachableValueMatrix(Map<Object, Set<Object>> valueToEntityMap, Map<Object, Set<Object>> valueToValueMap) {
        this.valueToEntityMap = valueToEntityMap;
        this.randomAccessValueToEntityMap = new IdentityHashMap<>(this.valueToEntityMap.size());
        for (var entry : this.valueToEntityMap.entrySet()) {
            randomAccessValueToEntityMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        this.valueToValueMap = valueToValueMap;
        this.randomAccessValueToValueMap = new IdentityHashMap<>(this.valueToValueMap.size());
        for (var entry : this.valueToValueMap.entrySet()) {
            randomAccessValueToValueMap.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        var first = valueToEntityMap.entrySet().stream().findFirst();
        this.valueClass = first.<Class<?>> map(entry -> entry.getKey().getClass()).orElse(null);
    }

    /**
     * @return all reachable values for the given value.
     */
    public @Nullable Set<Object> extractReachableEntities(Object value) {
        return valueToEntityMap.get(value);
    }

    /**
     * @return all reachable entities for the given value.
     */
    public @Nullable Set<Object> extractReachableValues(Object value) {
        return valueToValueMap.get(value);
    }

    public @Nullable List<Object> extractReachableEntitiesAsList(Object value) {
        return randomAccessValueToEntityMap.get(value);
    }

    public @Nullable List<Object> extractReachableValuesAsList(Object value) {
        return randomAccessValueToValueMap.get(value);
    }

    public int getSize() {
        return valueToEntityMap.size();
    }

    public boolean isValidValueClass(Object value) {
        if (valueToEntityMap.isEmpty()) {
            return false;
        }
        return Objects.requireNonNull(value).getClass().equals(valueClass);
    }

}
