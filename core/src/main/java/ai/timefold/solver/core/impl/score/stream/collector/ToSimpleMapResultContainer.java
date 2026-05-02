package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

final class ToSimpleMapResultContainer<Key_, Value_, Result_ extends Map<Key_, Value_>>
        implements ToMapResultContainer<Key_, Value_, Value_, Result_> {

    private final BinaryOperator<Value_> mergeFunction;
    private final Result_ result;
    private final Map<Key_, ToMapPerKeyCounter<Value_>> valueCounts = new HashMap<>(0);

    public ToSimpleMapResultContainer(Supplier<Result_> resultSupplier, BinaryOperator<Value_> mergeFunction) {
        this.mergeFunction = Objects.requireNonNull(mergeFunction);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    @Override
    public void add(Key_ key, Value_ value) {
        var counter = valueCounts.computeIfAbsent(key, k -> new ToMapPerKeyCounter<>());
        counter.add(value);
        result.put(key, counter.merge(mergeFunction));
    }

    @Override
    public void update(Key_ oldKey, Value_ oldValue, Key_ newKey, Value_ newValue) {
        if (Objects.equals(oldKey, newKey)) {
            var counter = valueCounts.get(oldKey);
            counter.update(oldValue, newValue);
            result.put(oldKey, counter.merge(mergeFunction));
        } else {
            remove(oldKey, oldValue);
            add(newKey, newValue);
        }
    }

    @Override
    public void remove(Key_ key, Value_ value) {
        var counter = valueCounts.get(key);
        counter.remove(value);
        if (counter.isEmpty()) {
            result.remove(key);
            valueCounts.remove(key);
        } else {
            result.put(key, counter.merge(mergeFunction));
        }
    }

    @Override
    public Result_ getResult() {
        return result;
    }

}
