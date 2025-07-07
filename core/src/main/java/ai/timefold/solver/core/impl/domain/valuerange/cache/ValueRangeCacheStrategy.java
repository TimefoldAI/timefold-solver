package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.List;

public sealed interface ValueRangeCacheStrategy<Value_> permits HashSetValueRangeCache, IdentityValueRangeCache {

    void add(Value_ value);

    Value_ get(int index);

    boolean contains(Value_ value);

    long getSize();

    List<Value_> getAll();

}