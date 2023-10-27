package ai.timefold.solver.core.api.score.stream;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollector;

final class DefaultBiConstraintCollector<A, B, ResultContainer_, Result_>
        implements BiConstraintCollector<A, B, ResultContainer_, Result_> {

    private final Supplier<ResultContainer_> supplier;
    private final TriFunction<ResultContainer_, A, B, Runnable> accumulator;
    private final Function<ResultContainer_, Result_> finisher;
    private final ConstraintCollectors.ConstraintCollectorKind collectorKind;
    private final Object[] equalityArgs;

    public DefaultBiConstraintCollector(Supplier<ResultContainer_> supplier,
            TriFunction<ResultContainer_, A, B, Runnable> accumulator,
            Function<ResultContainer_, Result_> finisher,
            ConstraintCollectors.ConstraintCollectorKind collectorKind,
            Object... equalityArgs) {
        this.supplier = supplier;
        this.accumulator = accumulator;
        this.finisher = finisher;
        this.collectorKind = collectorKind;
        this.equalityArgs = equalityArgs;
    }

    @Override
    public Supplier<ResultContainer_> supplier() {
        return supplier;
    }

    @Override
    public TriFunction<ResultContainer_, A, B, Runnable> accumulator() {
        return accumulator;
    }

    @Override
    public Function<ResultContainer_, Result_> finisher() {
        return finisher;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        DefaultBiConstraintCollector<?, ?, ?, ?> that = (DefaultBiConstraintCollector<?, ?, ?, ?>) object;
        return collectorKind == that.collectorKind && Arrays.equals(equalityArgs, that.equalityArgs);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(collectorKind);
        result = 31 * result + Arrays.hashCode(equalityArgs);
        return result;
    }
}
