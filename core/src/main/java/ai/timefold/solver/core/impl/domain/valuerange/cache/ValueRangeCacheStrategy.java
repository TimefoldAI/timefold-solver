package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.List;

import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;

import org.jspecify.annotations.NonNull;

public sealed interface ValueRangeCacheStrategy<Value_> permits HashSetValueRangeCache, IdentityValueRangeCache {

    void add(@NonNull Value_ value);

    Value_ get(int index);

    boolean contains(@NonNull Value_ value);

    long getSize();

    @NonNull
    List<Value_> getAll();

    /**
     * Merges the values from {@code otherCache} into the current instance.
     * This method creates a unique list of values
     * by combining the current cache instance with the one passed as an argument.
     */
    default void merge(@NonNull CountableValueRange<Value_> otherCache) {
        otherCache.createOriginalIterator().forEachRemaining(value -> {
            if (value != null) {
                this.add(value);
            }
        });
    }

}