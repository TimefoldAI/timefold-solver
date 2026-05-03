package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

final class ToMultiMapResultContainer<Key_, Value_, Set_ extends Set<Value_>, Result_ extends Map<Key_, Set_>>
        implements ToMapResultContainer<Key_, Value_, Set_, Result_> {

    private final Supplier<Set_> setSupplier;
    private final Result_ result;
    private final Map<Key_, ToMapPerKeyCounter<Key_, Value_>> valueCounts = new HashMap<>(0);
    private @Nullable ToMapPerKeyCounter<Key_, Value_> lastCounter;
    private @Nullable CountHolder<Value_> lastHolder;

    ToMultiMapResultContainer(Supplier<Result_> resultSupplier, IntFunction<Set_> setFunction) {
        var nonNullSetFunction = Objects.requireNonNull(setFunction);
        this.setSupplier = () -> nonNullSetFunction.apply(0);
        this.result = Objects.requireNonNull(resultSupplier).get();
    }

    @Override
    public void add(Key_ key, Value_ value) {
        lastCounter = valueCounts.computeIfAbsent(key, ToMapPerKeyCounter::new);
        lastHolder = lastCounter.add(value);
        if (lastHolder.count == 1) {
            result.computeIfAbsent(key, k -> setSupplier.get()).add(value);
        }
    }

    @Override
    public void update(ToMapPerKeyCounter<Key_, Value_> counter, CountHolder<Value_> holder,
            Key_ newKey, Value_ newValue) {
        if (Objects.equals(counter.key, newKey) && Objects.equals(holder.value, newValue)) {
            lastCounter = counter;
            lastHolder = holder;
            return;
        }
        if (Objects.equals(counter.key, newKey)) {
            counter.decrement(holder);
            if (holder.count == 0) {
                result.get(counter.key).remove(holder.value);
            }
            lastHolder = counter.add(newValue);
            if (lastHolder.count == 1) {
                result.computeIfAbsent(counter.key, k -> setSupplier.get()).add(newValue);
            }
            lastCounter = counter;
        } else {
            remove(counter, holder);
            add(newKey, newValue);
        }
    }

    @Override
    public void remove(ToMapPerKeyCounter<Key_, Value_> counter, CountHolder<Value_> holder) {
        counter.decrement(holder);
        if (holder.count == 0) {
            result.get(counter.key).remove(holder.value);
        }
        if (counter.isEmpty()) {
            valueCounts.remove(counter.key);
            result.remove(counter.key);
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
