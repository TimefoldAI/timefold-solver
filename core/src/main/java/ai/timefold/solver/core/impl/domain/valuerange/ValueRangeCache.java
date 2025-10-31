package ai.timefold.solver.core.impl.domain.valuerange;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Instances should be created using the {@link Builder} enum.
 *
 * @param <Value_>
 */
@NullMarked
public final class ValueRangeCache<Value_>
        implements Iterable<Value_> {

    private final List<Value_> valuesWithFastRandomAccess;
    private final Set<Value_> valuesWithFastLookup;
    private final CacheType cacheType;

    private ValueRangeCache(int size, Set<Value_> emptyCacheSet, CacheType cacheType) {
        this.valuesWithFastRandomAccess = new ArrayList<>(size);
        this.valuesWithFastLookup = emptyCacheSet;
        this.cacheType = cacheType;
    }

    private ValueRangeCache(Collection<Value_> collection, Set<Value_> emptyCacheSet, CacheType cacheType) {
        this.valuesWithFastRandomAccess = new ArrayList<>(collection);
        this.valuesWithFastLookup = emptyCacheSet;
        this.valuesWithFastLookup.addAll(valuesWithFastRandomAccess);
        this.cacheType = cacheType;
    }

    public void add(@Nullable Value_ value) {
        if (valuesWithFastLookup.add(value)) {
            valuesWithFastRandomAccess.add(value);
        }
    }

    public Value_ get(int index) {
        if (index < 0 || index >= valuesWithFastRandomAccess.size()) {
            throw new IndexOutOfBoundsException("Index: %d, Size: %d".formatted(index, valuesWithFastRandomAccess.size()));
        }
        return valuesWithFastRandomAccess.get(index);
    }

    public boolean contains(@Nullable Value_ value) {
        return valuesWithFastLookup.contains(value);
    }

    public long getSize() {
        return valuesWithFastRandomAccess.size();
    }

    /**
     * Iterates in original order of the values as provided, terminates when the last value is reached.
     */
    public Iterator<Value_> iterator() {
        return valuesWithFastRandomAccess.iterator();
    }

    /**
     * Iterates in random order, does not terminate.
     */
    public Iterator<Value_> iterator(Random workingRandom) {
        return new CachedListRandomIterator<>(valuesWithFastRandomAccess, workingRandom);
    }

    /**
     * Creates a copy of the cache and apply a sorting operation.
     *
     * @param sorter never null, the sorter
     */
    public ValueRangeCache<Value_> sort(ValueRangeSorter<Value_> sorter) {
        var valuesWithFastRandomAccessSorted = new ArrayList<>(valuesWithFastRandomAccess);
        sorter.sort(valuesWithFastRandomAccessSorted);
        return switch (cacheType) {
            case USER_VALUES -> Builder.FOR_USER_VALUES.buildCache(valuesWithFastRandomAccessSorted);
            case TRUSTED_VALUES -> Builder.FOR_TRUSTED_VALUES.buildCache(valuesWithFastRandomAccessSorted);
        };
    }

    public enum Builder {

        /**
         * Use when {@link #FOR_TRUSTED_VALUES} is not suitable.
         */
        FOR_USER_VALUES {
            @Override
            public <Value_> ValueRangeCache<Value_> buildCache(int size) {
                return new ValueRangeCache<>(size, CollectionUtils.newIdentityHashSet(size), CacheType.USER_VALUES);
            }

            @Override
            public <Value_> ValueRangeCache<Value_> buildCache(Collection<Value_> collection) {
                return new ValueRangeCache<>(collection, CollectionUtils.newIdentityHashSet(collection.size()),
                        CacheType.USER_VALUES);
            }

        },
        /**
         * For types where we can trust that {@link Object#equals(Object)} means
         * that if two objects are equal, they are the same object.
         * For example, this is the case for {@link String}, {@link Number}, {@link OffsetDateTime}
         * and many other JDK types.
         * It is not guaranteed to be the case for user-defined types,
         * which is when {@link #FOR_USER_VALUES} should be used instead.
         */
        FOR_TRUSTED_VALUES {
            @Override
            public <Value_> ValueRangeCache<Value_> buildCache(int size) {
                return new ValueRangeCache<>(size, CollectionUtils.newHashSet(size), CacheType.TRUSTED_VALUES);
            }

            @Override
            public <Value_> ValueRangeCache<Value_> buildCache(Collection<Value_> collection) {
                return new ValueRangeCache<>(collection, CollectionUtils.newHashSet(collection.size()),
                        CacheType.TRUSTED_VALUES);
            }

        };

        public abstract <Value_> ValueRangeCache<Value_> buildCache(int size);

        public abstract <Value_> ValueRangeCache<Value_> buildCache(Collection<Value_> collection);

    }

    private enum CacheType {
        USER_VALUES,
        TRUSTED_VALUES
    }

}
