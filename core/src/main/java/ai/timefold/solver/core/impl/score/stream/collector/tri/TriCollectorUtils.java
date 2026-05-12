package ai.timefold.solver.core.impl.score.stream.collector.tri;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.tri.TriConstraintCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class TriCollectorUtils {

    public static <ResultContainer_, A, B, C> QuadFunction<ResultContainer_, A, B, C, Runnable>
            fromIncremental(TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> incrementalAccumulator) {
        if (incrementalAccumulator instanceof TriFromAccumulatorAdapter<ResultContainer_, A, B, C>(QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator)) {
            return accumulator;
        }
        return new TriFromIncrementalAdapter<>(incrementalAccumulator);
    }

    public static <ResultContainer_, A, B, C> TriConstraintCollectorAccumulator<ResultContainer_, A, B, C>
            toIncremental(QuadFunction<ResultContainer_, A, B, C, Runnable> accumulator) {
        if (accumulator instanceof TriFromIncrementalAdapter<ResultContainer_, A, B, C>(TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> incrementalAccumulator)) {
            return incrementalAccumulator;
        }
        return new TriFromAccumulatorAdapter<>(accumulator);
    }

    private record TriFromIncrementalAdapter<ResultContainer_, A, B, C>(
            TriConstraintCollectorAccumulator<ResultContainer_, A, B, C> inc)
            implements
                QuadFunction<ResultContainer_, A, B, C, Runnable> {

        @Override
        public Runnable apply(ResultContainer_ container, A a, B b, C c) {
            var val = inc.intoGroup(container);
            val.add(a, b, c);
            return val::remove;
        }
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
        public void update(@Nullable A a, @Nullable B b, @Nullable C c) {
            undo.run();
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
