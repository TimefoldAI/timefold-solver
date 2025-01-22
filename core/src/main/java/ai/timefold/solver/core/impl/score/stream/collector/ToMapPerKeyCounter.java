package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public final class ToMapPerKeyCounter<Value_> {

    private final Map<Value_, Long> counts = new LinkedHashMap<>(0);

    public long add(Value_ value) {
        return counts.compute(value, (k, currentCount) -> {
            if (currentCount == null) {
                return 1L;
            } else {
                return currentCount + 1;
            }
        });
    }

    public long remove(Value_ value) {
        Long newCount = counts.compute(value, (k, currentCount) -> {
            if (currentCount > 1L) {
                return currentCount - 1;
            } else {
                return null;
            }
        });
        return newCount == null ? 0L : newCount;
    }

    public Value_ merge(BinaryOperator<Value_> mergeFunction) {
        // Rebuilding the value from the collection is not incremental.
        // The impact is negligible, assuming there are not too many values for the same key.
        return counts.entrySet()
                .stream()
                .map(e -> Stream.generate(e::getKey)
                        .limit(e.getValue())
                        .reduce(mergeFunction)
                        .orElseThrow(() -> new IllegalStateException("Impossible state: Should have had at least one value.")))
                .reduce(mergeFunction)
                .orElseThrow(() -> new IllegalStateException("Impossible state: Should have had at least one value."));
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

}
