package ai.timefold.solver.core.impl.score.stream.collector.quad;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class QuadCollectorUtils {

    public static <ResultContainer_, A, B, C, D> QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D>
            toIncremental(PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator) {
        if (accumulator instanceof QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> inc) {
            return inc;
        }
        return new QuadFromAccumulatorAdapter<>(accumulator);
    }

    private record QuadFromAccumulatorAdapter<ResultContainer_, A, B, C, D>(
            PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator)
            implements
                QuadConstraintCollectorAccumulator<ResultContainer_, A, B, C, D> {

        @Override
        public QuadConstraintCollectorValueHandle<A, B, C, D> intoGroup(ResultContainer_ container) {
            return new QuadValueHandle<>(accumulator, container);
        }
    }

    private static final class QuadValueHandle<ResultContainer_, A, B, C, D>
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {
        private final PentaFunction<ResultContainer_, @Nullable A, @Nullable B, @Nullable C, @Nullable D, Runnable> accumulator;
        private final ResultContainer_ container;
        private @Nullable Runnable undo;

        QuadValueHandle(PentaFunction<ResultContainer_, A, B, C, D, Runnable> accumulator,
                ResultContainer_ container) {
            this.accumulator = accumulator;
            this.container = container;
        }

        @Override
        public void add(@Nullable A a, @Nullable B b, @Nullable C c, @Nullable D d) {
            undo = accumulator.apply(container, a, b, c, d);
        }

        @Override
        public void remove() {
            undo.run();
            undo = null;
        }
    }

    private QuadCollectorUtils() {
    }

}
