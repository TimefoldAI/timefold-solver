package ai.timefold.solver.core.impl.score.stream;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public final class ToMapPerKeyCounter<Value> {

    private final Map<Value, Long> counts = new LinkedHashMap<>(0);

    public long add(Value value) {
        return counts.compute(value, (k, currentCount) -> {
            if (currentCount == null) {
                return 1L;
            } else {
                return currentCount + 1;
            }
        });
    }

    public long remove(Value value) {
        Long newCount = counts.compute(value, (k, currentCount) -> {
            if (currentCount > 1L) {
                return currentCount - 1;
            } else {
                return null;
            }
        });
        return newCount == null ? 0L : newCount;
    }

    public Value merge(BinaryOperator<Value> mergeFunction) {
        // Rebuilding the value from the collection is not incremental.
        // The impact is negligible, assuming there are not too many values for the same key.
        return counts.entrySet()
                .stream()
                .flatMap(e -> Stream.generate(e::getKey).limit(e.getValue()))
                .reduce(mergeFunction)
                .orElseThrow(() -> new IllegalStateException("Programming error: Should have had at least one value."));
    }

    public boolean isEmpty() {
        return counts.isEmpty();
    }

}
