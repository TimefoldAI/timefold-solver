package ai.timefold.solver.core.impl.score.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class ToSimpleMapResultContainer<Key, Value, Result_ extends Map<Key, Value>>
        implements ToMapResultContainer<Key, Value, Value, Result_> {

    private final BinaryOperator<Value> mergeFunction;
    private final Result_ result;
    private final Map<Key, ToMapPerKeyCounter<Value>> valueCounts = new HashMap<>(0);

    public ToSimpleMapResultContainer(Supplier<Result_> resultSupplier, BinaryOperator<Value> mergeFunction) {
        this.mergeFunction = Objects.requireNonNull(mergeFunction);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    public ToSimpleMapResultContainer(IntFunction<Result_> resultSupplier, BinaryOperator<Value> mergeFunction) {
        this.mergeFunction = Objects.requireNonNull(mergeFunction);
        this.result = Objects.requireNonNull(resultSupplier).apply(0);
    }

    @Override
    public void add(Key key, Value value) {
        ToMapPerKeyCounter<Value> counter = valueCounts.computeIfAbsent(key, k -> new ToMapPerKeyCounter<>());
        long newCount = counter.add(value);
        if (newCount == 1L) {
            result.put(key, value);
        } else {
            result.put(key, counter.merge(mergeFunction));
        }
    }

    @Override
    public void remove(Key key, Value value) {
        ToMapPerKeyCounter<Value> counter = valueCounts.get(key);
        long newCount = counter.remove(value);
        if (newCount == 0L) {
            result.remove(key);
        } else {
            result.put(key, counter.merge(mergeFunction));
        }
        if (counter.isEmpty()) {
            valueCounts.remove(key);
        }
    }

    @Override
    public Result_ getResult() {
        return result;
    }

}
