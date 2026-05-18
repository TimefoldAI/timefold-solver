package ai.timefold.solver.core.impl.score.stream.collector.bi;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.bi.BiConstraintCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BiCollectorUtils {

    public static <ResultContainer_, A, B> BiConstraintCollectorAccumulator<ResultContainer_, A, B>
            toIncremental(TriFunction<ResultContainer_, A, B, Runnable> accumulator) {
        if (accumulator instanceof BiConstraintCollectorAccumulator<ResultContainer_, A, B> inc) {
            return inc;
        }
        return new BiFromAccumulatorAdapter<>(accumulator);
    }

    private record BiFromAccumulatorAdapter<ResultContainer_, A, B>(TriFunction<ResultContainer_, A, B, Runnable> accumulator)
            implements
                BiConstraintCollectorAccumulator<ResultContainer_, A, B> {

        @Override
        public BiConstraintCollectorValueHandle<A, B> intoGroup(ResultContainer_ container) {
            return new BiValueHandle<>(accumulator, container);
        }
    }

    private static final class BiValueHandle<ResultContainer_, A, B>
            implements BiConstraintCollectorValueHandle<A, B> {
        private final TriFunction<ResultContainer_, @Nullable A, @Nullable B, Runnable> accumulator;
        private final ResultContainer_ container;
        private @Nullable Runnable undo;

        BiValueHandle(TriFunction<ResultContainer_, A, B, Runnable> accumulator, ResultContainer_ container) {
            this.accumulator = accumulator;
            this.container = container;
        }

        @Override
        public void add(@Nullable A a, @Nullable B b) {
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
