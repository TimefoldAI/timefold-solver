package ai.timefold.solver.core.impl.domain.valuerange.cache;

public interface ValueRangeCacheStrategy<Value_> {

    void add(Value_ value);

    Value_ get(int index);

    boolean contains(Value_ value);

    long getSize();

    default ValueRangeCacheStrategy<Value_> merge(ValueRangeCacheStrategy<Value_> other) {
        for (var i = 0; i < other.getSize(); i++) {
            add(other.get(i));
        }
        return this;
    }
}
