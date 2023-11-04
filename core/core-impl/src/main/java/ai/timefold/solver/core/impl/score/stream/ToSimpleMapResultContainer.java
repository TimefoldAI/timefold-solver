package ai.timefold.solver.core.impl.score.stream;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class ToSimpleMapResultContainer<Key_, Value_, Result_ extends Map<Key_, Value_>>
        implements ToMapResultContainer<Key_, Value_, Value_, Result_> {

    private final BinaryOperator<Value_> mergeFunction;
    private final Result_ result;
    private final Map<Key_, ToMapPerKeyCounter<Value_>> valueCounts = new HashMap<>(0);

    public ToSimpleMapResultContainer(Supplier<Result_> resultSupplier, BinaryOperator<Value_> mergeFunction) {
        this.mergeFunction = Objects.requireNonNull(mergeFunction);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    public ToSimpleMapResultContainer(IntFunction<Result_> resultSupplier, BinaryOperator<Value_> mergeFunction) {
        this.mergeFunction = Objects.requireNonNull(mergeFunction);
        this.result = Objects.requireNonNull(resultSupplier).apply(0);
    }

    @Override
    public void add(Key_ key, Value_ value) {
        ToMapPerKeyCounter<Value_> counter = valueCounts.computeIfAbsent(key, k -> new ToMapPerKeyCounter<>());
        long newCount = counter.add(value);
        if (newCount == 1L) {
            result.put(key, value);
        } else {
            result.put(key, counter.merge(mergeFunction));
        }
    }

    @Override
    public void remove(Key_ key, Value_ value) {
        ToMapPerKeyCounter<Value_> counter = valueCounts.get(key);
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
