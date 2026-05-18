package ai.timefold.solver.core.impl.score.stream.collector.tri;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class TriCollectorUtils {

    public static <ResultContainer_, A, B, C> TriConstraintCollectorAccumulator<ResultContainer_, A, B, C>
            toIncremental(QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator) {
        if (accumulator instanceof TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> inc) {
            return inc;
        }
        return new TriFromAccumulatorAdapter<>(accumulator);
    }

    private record TriFromAccumulatorAdapter<ResultContainer_, A, B, C>(
            QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator)
            implements
                TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> {

        @Override
        public TriConstraintCollectorValueHandle<A, B, C> intoGroup(ResultContainer_ container) {
            return new TriValueHandle<>(accumulator, container);
        }
    }

    private static final class TriValueHandle<ResultContainer_, A, B, C>
            implements TriConstraintCollectorValueHandle<A, B, C> {
        private final QuadFunction<ResultContainer_, @Nullable A, @Nullable B, @Nullable C, Runnable> accumulator;
        private final ResultContainer_ container;
        private @Nullable Runnable undo;

        TriValueHandle(QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator, ResultContainer_ container) {
            this.accumulator = accumulator;
            this.container = container;
        }

        @Override
        public void add(@Nullable A a, @Nullable B b, @Nullable C c) {
            undo = accumulator.apply(container, a, b, c);
        }

        @Override
        public void remove() {
            undo.run();
            undo = null;
        }
    }

    private TriCollectorUtils() {
    }

}
