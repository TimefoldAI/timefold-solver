package ai.timefold.solver.core.impl.score.stream.collector.quad;

import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollector;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorAccumulator;
import ai.timefold.solver.core.api.score.stream.quad.QuadConstraintCollectorValueHandle;
import ai.timefold.solver.core.impl.score.stream.collector.AbstractCountSlot;
import ai.timefold.solver.core.impl.util.MutableLong;

import org.jspecify.annotations.NonNull;

final class CountQuadCollector<A, B, C, D>
        implements QuadConstraintCollector<A, B, C, D, MutableLong, Long> {
    private static final CountQuadCollector<?, ?, ?, ?> INSTANCE = new CountQuadCollector<>();

    private CountQuadCollector() {
    }

    @SuppressWarnings("unchecked")
    static <A, B, C, D> CountQuadCollector<A, B, C, D> getInstance() {
        return (CountQuadCollector<A, B, C, D>) INSTANCE;
    }

    @Override
    public @NonNull Supplier<MutableLong> supplier() {
        return MutableLong::new;
    }

    @Override
    public @NonNull QuadConstraintCollectorAccumulator<MutableLong, A, B, C, D> accumulator() {
        return Slot::new;
    }

    @Override
    public @NonNull Function<MutableLong, Long> finisher() {
        return MutableLong::longValue;
    }

    private static final class Slot<A, B, C, D> extends AbstractCountSlot
            implements QuadConstraintCollectorValueHandle<A, B, C, D> {

        Slot(MutableLong state) {
            super(state);
        }

        @Override
        public void add(A a, B b, C c, D d) {
            addMapped();
        }

        @Override
        public void replaceWith(A a, B b, C c, D d) {
            replaceWithMapped();
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
