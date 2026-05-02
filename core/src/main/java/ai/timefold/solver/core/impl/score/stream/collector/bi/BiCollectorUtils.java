package ai.timefold.solver.core.impl.score.stream.collector.bi;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BiCollectorUtils {

    public static <ResultContainer_, A, B> TriFunction<ResultContainer_, A, B, Runnable>
            fromIncremental(BiConstraintCollectorAccumulator<ResultContainer_, A, B> incrementalAccumulator) {
        if (incrementalAccumulator instanceof BiFromAccumulatorAdapter<ResultContainer_, A, B>(TriFunction<ResultContainer_, A, B, Runnable> accumulator)) {
            return accumulator;
        }
        return new BiFromIncrementalAdapter<>(incrementalAccumulator);
    }

    public static <ResultContainer_, A, B> BiConstraintCollectorAccumulator<ResultContainer_, A, B>
            toIncremental(TriFunction<ResultContainer_, A, B, Runnable> accumulator) {
        if (accumulator instanceof BiFromIncrementalAdapter<ResultContainer_, A, B>(BiConstraintCollectorAccumulator<ResultContainer_, A, B> incrementalAccumulator)) {
            return incrementalAccumulator;
        }
        return new BiFromAccumulatorAdapter<>(accumulator);
    }

    private record BiFromIncrementalAdapter<ResultContainer_, A, B>(
            BiConstraintCollectorAccumulator<ResultContainer_, A, B> inc)
            implements
                TriFunction<ResultContainer_, A, B, Runnable> {

        @Override
        public Runnable apply(ResultContainer_ container, A a, B b) {
            var val = inc.intoGroup(container);
            val.add(a, b);
            return val::remove;
        }
    }

    private record BiFromAccumulatorAdapter<ResultContainer_, A, B>(TriFunction<ResultContainer_, A, B, Runnable> accumulator)
            implements
                BiConstraintCollectorAccumulator<ResultContainer_, A, B> {

        @Override
        public BiConstraintCollectorAccumulatedValue<A, B> intoGroup(ResultContainer_ container) {
            return new BiAccumulatedValue<>(accumulator, container);
        }
    }

    private static final class BiAccumulatedValue<ResultContainer_, A, B>
            implements BiConstraintCollectorAccumulatedValue<A, B> {
        private final TriFunction<ResultContainer_, @Nullable A, @Nullable B, Runnable> accumulator;
        private final ResultContainer_ container;
        private @Nullable Runnable undo;

        BiAccumulatedValue(TriFunction<ResultContainer_, A, B, Runnable> accumulator, ResultContainer_ container) {
            this.accumulator = accumulator;
            this.container = container;
        }

        @Override
        public void add(@Nullable A a, @Nullable B b) {
            undo = accumulator.apply(container, a, b);
        }

        @Override
        public void update(@Nullable A a, @Nullable B b) {
            undo.run();
            undo = accumulator.apply(container, a, b);
        }

        @Override
        public void remove() {
            undo.run();
            undo = null;
        }
    }

    private BiCollectorUtils() {
    }

}
