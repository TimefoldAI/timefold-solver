package ai.timefold.solver.core.impl.score.stream;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class SequenceAccumulator<Result_> {

    private final ToIntFunction<Result_> indexMap;

    public SequenceAccumulator(ToIntFunction<Result_> indexMap) {
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    public Supplier<ConsecutiveSetTree<Result_, Integer, Integer>> getContextSupplier() {
        return () -> new ConsecutiveSetTree<>(
                (Integer a, Integer b) -> b - a,
                Integer::sum, 1, 0);
    }

    public Runnable accumulate(ConsecutiveSetTree<Result_, Integer, Integer> context, Result_ result) {
        var value = indexMap.applyAsInt(result);
        context.add(result, value);
        return () -> context.remove(result);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SequenceAccumulator<?> other) {
            return Objects.equals(indexMap, other.indexMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return indexMap.hashCode();
    }
}
