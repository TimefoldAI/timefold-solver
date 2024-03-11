package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class ToMultiMapResultContainer<Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
        implements ToMapResultContainer<Key_, Value_, Set_, Result_> {

    private final Supplier<Set_> setSupplier;
    private final Result_ result;
    private final Map<Key_, ToMapPerKeyCounter<Value_>> valueCounts = new HashMap<>(0);

    public ToMultiMapResultContainer(Supplier<Result_> resultSupplier, IntFunction<Set_> setFunction) {
        IntFunction<Set_> nonNullSetFunction = Objects.requireNonNull(setFunction);
        this.setSupplier = () -> nonNullSetFunction.apply(0);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    public ToMultiMapResultContainer(IntFunction<Result_> resultFunction, IntFunction<Set_> setFunction) {
        IntFunction<Set_> nonNullSetFunction = Objects.requireNonNull(setFunction);
        this.setSupplier = () -> nonNullSetFunction.apply(0);
        this.result = Objects.requireNonNull(resultFunction).apply(0);
    }

    @Override
    public void add(Key_ key, Value_ value) {
        ToMapPerKeyCounter<Value_> counter = valueCounts.computeIfAbsent(key, k -> new ToMapPerKeyCounter<>());
        counter.add(value);
        result.computeIfAbsent(key, k -> setSupplier.get())
                .add(value);
    }

    @Override
    public void remove(Key_ key, Value_ value) {
        ToMapPerKeyCounter<Value_> counter = valueCounts.get(key);
        long newCount = counter.remove(value);
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
