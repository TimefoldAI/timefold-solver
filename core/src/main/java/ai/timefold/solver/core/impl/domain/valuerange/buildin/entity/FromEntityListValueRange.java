package ai.timefold.solver.core.impl.domain.valuerange.buildin.entity;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.valuerange.AbstractCountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.cache.ValueRangeCacheStrategy;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromListVarEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;

import org.jspecify.annotations.NonNull;

/**
 * The value range is built based on the individual value ranges of each entity.
 * The implementation uses an on-demand loading list, which loads items based on user requests.
 * The main goal is to avoid loading values from all entities at once,
 * instead opting for incremental data loading as needed.
 * 
 * @param <Value_> the value type
 */
public final class FromEntityListValueRange<Value_> extends AbstractCountableValueRange<Value_> {

    private final List<Value_> valueList;

    public <Solution_> FromEntityListValueRange(List<?> entityList, int size,
                                                FromListVarEntityPropertyValueRangeDescriptor<Solution_> valueRangeDescriptor) {
        this.valueList = new OnDemandValueEntityList<>(entityList, size, valueRangeDescriptor);
    }

    @Override
    public long getSize() {
        return valueList.size();
    }

    @Override
    public Value_ get(long index) {
        return valueList.get((int) index);
    }

    @Override
    public boolean contains(Value_ value) {
        return valueList.contains(value);
    }

    @Override
    public @NonNull Iterator<Value_> createOriginalIterator() {
        return valueList.iterator();
    }

    @Override
    public @NonNull Iterator<Value_> createRandomIterator(@NonNull Random workingRandom) {
        return new CachedListRandomIterator<>(valueList, workingRandom);
    }

    @Override
    public String toString() {
        return valueList.isEmpty() ? "[]" : "[" + valueList.get(0) + "-" + valueList.get(valueList.size() - 1) + "]";
    }

    private static class OnDemandValueEntityList<Value_> extends AbstractList<Value_> {

        private final List<?> entityList;
        private final FromListVarEntityPropertyValueRangeDescriptor<?> valueRangeDescriptor;
        private final ValueRangeCacheStrategy<Value_> cacheStrategy;
        private final int size;
        private int currentEntityIndex = 0;
        private boolean fullyLoaded = false;

        public <Solution_> OnDemandValueEntityList(List<?> entityList, int size,
                FromListVarEntityPropertyValueRangeDescriptor<Solution_> valueRangeDescriptor) {
            this.entityList = entityList;
            this.size = size;
            this.valueRangeDescriptor = valueRangeDescriptor;
            // We initialize the cache using the value range from the first entity
            if (entityList.isEmpty()) {
                throw new IllegalArgumentException("Impossible state: the entity list (%s) cannot be empty."
                        .formatted(valueRangeDescriptor.getVariableDescriptor().getEntityDescriptor().getEntityClass()
                                .getSimpleName()));
            }
            var firstValueRange = valueRangeDescriptor.<Value_> extractValueRange(null, entityList.get(0));
            this.cacheStrategy = firstValueRange.generateCache();
        }

        @Override
        public Value_ get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(index);
            }
            if (!fullyLoaded) {
                ensureLoadedAtIndex(index);
            }
            return cacheStrategy.get(index);
        }

        private void ensureLoadedAtIndex(int index) {
            while (currentEntityIndex < entityList.size() && index <= cacheStrategy.getSize()) {
                var entity = entityList.get(currentEntityIndex++);
                readEntity(entity);
            }
            this.fullyLoaded = currentEntityIndex == entityList.size();
        }

        private void readEntity(Object entity) {
            var otherValueRange = valueRangeDescriptor.<Value_> extractValueRange(null, entity);
            cacheStrategy.merge(otherValueRange.generateCache());
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof OnDemandValueEntityList<?> that)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            return Objects.equals(entityList, that.entityList)
                    && Objects.equals(valueRangeDescriptor, that.valueRangeDescriptor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityList, valueRangeDescriptor);
        }
    }

}
