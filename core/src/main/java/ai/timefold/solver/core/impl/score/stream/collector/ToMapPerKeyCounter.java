package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

final class ToMapPerKeyCounter<Key_, Value_> {

    final Key_ key;
    private final Map<Value_, CountHolder<Value_>> counts = new LinkedHashMap<>(0);

    ToMapPerKeyCounter(Key_ key) {
        this.key = key;
    }

    CountHolder<Value_> add(Value_ value) {
        var holder = counts.get(value);
        if (holder == null) {
            holder = new CountHolder<>(value);
            counts.put(value, holder);
        } else {
            holder.count++;
        }
        return holder;
    }

    void decrement(CountHolder<Value_> holder) {
        holder.count--;
        if (holder.count == 0) {
            counts.remove(holder.value);
        }
    }

    Value_ merge(BinaryOperator<Value_> mergeFunction) {
        return counts.values()
                .stream()
                .map(h -> Stream.generate(() -> h.value)
                        .limit(h.count)
                        .reduce(mergeFunction)
                        .orElseThrow(
                                () -> new IllegalStateException("Impossible state: Should have had at least one value.")))
                .reduce(mergeFunction)
                .orElseThrow(() -> new IllegalStateException("Impossible state: Should have had at least one value."));
    }

    boolean isEmpty() {
        return counts.isEmpty();
    }

}
