package ai.timefold.solver.core.impl.domain.valuerange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.domain.valuerange.sort.ValueRangeSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.CachedListRandomIterator;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ValueRangeCache<Value_>
        implements Iterable<Value_> {

    public static <Value_> ValueRangeCache<Value_> of(int size) {
        return new ValueRangeCache<>(size, CollectionUtils.newHashSet(size));
    }

    public static <Value_> ValueRangeCache<Value_> of(Collection<Value_> collection) {
        return new ValueRangeCache<>(collection, CollectionUtils.newHashSet(collection.size()));
    }

    public static <Value_> ValueRangeCache<Value_> of(List<Value_> valuesWithFastRandomAccess,
            Set<Value_> valuesWithFastLookup) {
        return new ValueRangeCache<>(valuesWithFastRandomAccess, valuesWithFastLookup);
    }

    private final List<Value_> valuesWithFastRandomAccess;
    private final Set<Value_> valuesWithFastLookup;

    private ValueRangeCache(int size, Set<Value_> emptyCacheSet) {
        this.valuesWithFastRandomAccess = new ArrayList<>(size);
        this.valuesWithFastLookup = emptyCacheSet;
    }

    private ValueRangeCache(Collection<Value_> collection, Set<Value_> emptyCacheSet) {
        this.valuesWithFastRandomAccess = new ArrayList<>(collection);
        this.valuesWithFastLookup = emptyCacheSet;
        this.valuesWithFastLookup.addAll(valuesWithFastRandomAccess);
    }

    private ValueRangeCache(List<Value_> valuesWithFastRandomAccess, Set<Value_> valuesWithFastLookup) {
        this.valuesWithFastRandomAccess = valuesWithFastRandomAccess;
        this.valuesWithFastLookup = valuesWithFastLookup;
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
    public Iterator<Value_> iterator(RandomGenerator workingRandom) {
        return new CachedListRandomIterator<>(valuesWithFastRandomAccess, workingRandom);
    }

    /**
     * Creates a copy of the cache and apply a sorting operation.
     *
     * @param sorter never null, the sorter
     */
    public ValueRangeCache<Value_> sort(ValueRangeSorter<Value_> sorter) {
        // We need to copy the list to ensure it won't affect other cache instances
        var newValuesWithFastRandomAccess = new ArrayList<>(valuesWithFastRandomAccess);
        sorter.sort(newValuesWithFastRandomAccess);
        return ValueRangeCache.of(newValuesWithFastRandomAccess, valuesWithFastLookup);
    }

}
