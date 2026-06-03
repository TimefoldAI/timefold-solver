package ai.timefold.solver.core.impl.neighborhood.stream.collector;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.UniNeighborhoodsCollectorAccumulator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.UniNeighborhoodsCollectorValueHandle;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ToListUniNeighborhoodsCollector<Solution_, A>
        implements UniNeighborhoodsCollector<Solution_, A, AbstractToListSlot.State<A>, List<A>> {

    private static final ToListUniNeighborhoodsCollector<?, ?> INSTANCE = new ToListUniNeighborhoodsCollector<>();

    @SuppressWarnings("unchecked")
    public static <Solution_, A> ToListUniNeighborhoodsCollector<Solution_, A> create() {
        return (ToListUniNeighborhoodsCollector<Solution_, A>) INSTANCE;
    }

    private ToListUniNeighborhoodsCollector() {
    }

    @Override
    public Supplier<AbstractToListSlot.State<A>> supplier() {
        return AbstractToListSlot.State::new;
    }

    @Override
    public UniNeighborhoodsCollectorAccumulator<Solution_, A, AbstractToListSlot.State<A>> accumulator() {
        return (view, state) -> new Slot<>(state);
    }

    @Override
    public Function<AbstractToListSlot.State<A>, @Nullable List<A>> finisher() {
        return AbstractToListSlot.State::result;
    }

    private static final class Slot<A> extends AbstractToListSlot<A>
            implements UniNeighborhoodsCollectorValueHandle<A> {

        Slot(AbstractToListSlot.State<A> state) {
            super(state);
        }

        @Override
        public void add(@Nullable A a) {
            addMapped(a);
        }

        @Override
        public void replaceWith(@Nullable A a) {
            replaceWithMapped(a);
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
