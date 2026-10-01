package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

final class ToSimpleMapResultContainer<Key_, Value_, Result_ extends Map<Key_, Value_>>
        implements ToMapResultContainer<Key_, Value_, Value_, Result_> {

    private final BinaryOperator<Value_> mergeFunction;
    private final Result_ result;
    private final Map<Key_, ToMapPerKeyCounter<Key_, Value_>> valueCounts = new HashMap<>(0);
    private @Nullable ToMapPerKeyCounter<Key_, Value_> lastCounter;
    private @Nullable CountHolder<Value_> lastHolder;

    ToSimpleMapResultContainer(Supplier<Result_> resultSupplier, BinaryOperator<Value_> mergeFunction) {
        this.mergeFunction = Objects.requireNonNull(mergeFunction);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    @Override
    public void add(Key_ key, Value_ value) {
        lastCounter = valueCounts.computeIfAbsent(key, ToMapPerKeyCounter::new);
        lastHolder = lastCounter.add(value);
        result.put(key, lastCounter.merge(mergeFunction));
    }

    @Override
    public void replaceWith(ToMapPerKeyCounter<Key_, Value_> counter, CountHolder<Value_> holder,
            Key_ newKey, Value_ newValue) {
        if (Objects.equals(counter.key, newKey) && Objects.equals(holder.value, newValue)) {
            lastCounter = counter;
            lastHolder = holder;
            return;
        }
        if (Objects.equals(counter.key, newKey)) {
            counter.decrement(holder);
            lastHolder = counter.add(newValue);
            result.put(counter.key, counter.merge(mergeFunction));
            lastCounter = counter;
        } else {
            remove(counter, holder);
            add(newKey, newValue);
        }
    }

    @Override
    public void remove(ToMapPerKeyCounter<Key_, Value_> counter, CountHolder<Value_> holder) {
        counter.decrement(holder);
        if (counter.isEmpty()) {
            valueCounts.remove(counter.key);
            result.remove(counter.key);
        } else {
            result.put(counter.key, counter.merge(mergeFunction));
        }
    }

    @Override
    public @Nullable ToMapPerKeyCounter<Key_, Value_> lastCounter() {
        return lastCounter;
    }

    @Override
    public @Nullable CountHolder<Value_> lastHolder() {
        return lastHolder;
    }

    @Override
    public Result_ getResult() {
        return result;
    }

}
