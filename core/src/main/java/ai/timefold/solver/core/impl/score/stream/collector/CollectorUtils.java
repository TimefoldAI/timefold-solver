package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulatedValue;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollectorAccumulator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class CollectorUtils {

    public static <ResultContainer_, A> BiFunction<ResultContainer_, A, Runnable> fromIncrementalUni(
            UniConstraintCollectorAccumulator<ResultContainer_, A> incrementalAccumulator) {
        if (incrementalAccumulator instanceof UniFromAccumulatorAdapter<ResultContainer_, A>(BiFunction<ResultContainer_, A, Runnable> accumulator)) {
            return accumulator;
        }
        return new UniFromIncrementalAdapter<>(incrementalAccumulator);
    }

    public static <ResultContainer_, A> UniConstraintCollectorAccumulator<ResultContainer_, A> toIncrementalUni(
            BiFunction<ResultContainer_, A, Runnable> accumulator) {
        if (accumulator instanceof UniFromIncrementalAdapter<ResultContainer_, A>(UniConstraintCollectorAccumulator<ResultContainer_, A> incrementalAccumulator)) {
            return incrementalAccumulator;
        }
        return new UniFromAccumulatorAdapter<>(accumulator);
    }

    private record UniFromIncrementalAdapter<ResultContainer_, A>(UniConstraintCollectorAccumulator<ResultContainer_, A> inc)
            implements
                BiFunction<ResultContainer_, A, Runnable> {

        @Override
        public Runnable apply(ResultContainer_ container, A a) {
            var val = inc.intoGroup(container);
            val.add(a);
            return val::remove;
        }
    }

    private record UniFromAccumulatorAdapter<ResultContainer_, A>(BiFunction<ResultContainer_, A, Runnable> accumulator)
            implements
                UniConstraintCollectorAccumulator<ResultContainer_, A> {

        @Override
        public UniConstraintCollectorAccumulatedValue<A> intoGroup(ResultContainer_ container) {
            return new UniAccumulatedValue<>(accumulator, container);
        }
    }

    private static final class UniAccumulatedValue<ResultContainer_, A>
            implements UniConstraintCollectorAccumulatedValue<A> {
        private final BiFunction<ResultContainer_, @Nullable A, Runnable> accumulator;
        private final ResultContainer_ container;
        private @Nullable Runnable undo;

        UniAccumulatedValue(BiFunction<ResultContainer_, A, Runnable> accumulator, ResultContainer_ container) {
            this.accumulator = accumulator;
            this.container = container;
        }

        @Override
        public void add(@Nullable A a) {
            undo = accumulator.apply(container, a);
        }

        @Override
        public void update(@Nullable A a) {
            undo.run();
            undo = accumulator.apply(container, a);
        }

        @Override
        public void remove() {
            undo.run();
            undo = null;
        }
    }

    private CollectorUtils() {
    }

}
