package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class UniCollectorUtils {

    public static <ResultContainer_, A> UniConstraintCollectorAccumulator<ResultContainer_, A>
            toIncremental(BiFunction<ResultContainer_, A, Runnable> accumulator) {
        if (accumulator instanceof UniConstraintCollectorAccumulator<ResultContainer_, A> inc) {
            return inc;
        }
        return new UniFromAccumulatorAdapter<>(accumulator);
    }

    private record UniFromAccumulatorAdapter<ResultContainer_, A>(BiFunction<ResultContainer_, A, Runnable> accumulator)
            implements
                UniConstraintCollectorAccumulator<ResultContainer_, A> {

        @Override
        public UniConstraintCollectorValueHandle<A> intoGroup(ResultContainer_ container) {
            return new UniValueHandle<>(accumulator, container);
        }
    }

    private static final class UniValueHandle<ResultContainer_, A>
            implements UniConstraintCollectorValueHandle<A> {
        private final BiFunction<ResultContainer_, @Nullable A, Runnable> accumulator;
        private final ResultContainer_ container;
        private @Nullable Runnable undo;

        UniValueHandle(BiFunction<ResultContainer_, A, Runnable> accumulator, ResultContainer_ container) {
            this.accumulator = accumulator;
            this.container = container;
        }

        @Override
        public void add(@Nullable A a) {
            undo = accumulator.apply(container, a);
        }

        @Override
        public void remove() {
            undo.run();
            undo = null;
        }
    }

}
