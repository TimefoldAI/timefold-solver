package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

final class ToMultiMapResultContainer<Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
        implements ToMapResultContainer<Key_, Value_, Set_, Result_> {

    private final Supplier<Set_> setSupplier;
    private final Result_ result;
    private final Map<Key_, ToMapPerKeyCounter<Value_>> valueCounts = new HashMap<>();

    public ToMultiMapResultContainer(Supplier<Result_> resultSupplier, IntFunction<Set_> setFunction) {
        var nonNullSetFunction = Objects.requireNonNull(setFunction);
        this.setSupplier = () -> nonNullSetFunction.apply(0);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    @Override
    public void add(Key_ key, Value_ value) {
        var counter = valueCounts.computeIfAbsent(key, k -> new ToMapPerKeyCounter<>());
        counter.add(value);
        result.computeIfAbsent(key, k -> setSupplier.get())
                .add(value);
    }

    @Override
    public void update(Key_ oldKey, Value_ oldValue, Key_ newKey, Value_ newValue) {
        if (Objects.equals(oldKey, newKey)) {
            var counter = valueCounts.get(oldKey);
            var removedCount = counter.update(oldValue, newValue);
            if (removedCount == 0) {
                result.get(oldKey).remove(oldValue);
            }
            result.computeIfAbsent(oldKey, k -> setSupplier.get()).add(newValue);
        } else {
            remove(oldKey, oldValue);
            add(newKey, newValue);
        }
    }

    @Override
    public void remove(Key_ key, Value_ value) {
        var counter = valueCounts.get(key);
        var newCount = counter.remove(value);
        if (newCount == 0) {
            result.get(key).remove(value);
        }
        if (counter.isEmpty()) {
            valueCounts.remove(key);
            result.remove(key);
        }
    }

    @Override
    public Result_ getResult() {
        return result;
    }

}
