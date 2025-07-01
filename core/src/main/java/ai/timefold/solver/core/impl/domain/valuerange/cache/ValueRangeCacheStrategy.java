package ai.timefold.solver.core.impl.domain.valuerange.cache;

import java.util.List;

public interface ValueRangeCacheStrategy<Value_> {

    void add(Value_ value);

    Value_ get(int index);

    boolean contains(Value_ value);

    long getSize();

    List<Value_> getAll();

}