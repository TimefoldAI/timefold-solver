package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorAccumulator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorValueHandle;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ToListUniNeighborhoodsCollector<Solution_, A, Mapped_>
        implements UniNeighborhoodsCollector<Solution_, A, AbstractToListSlot.State<Mapped_>, List<Mapped_>> {

    private final UniNeighborhoodsMapper<Solution_, A, Mapped_> mapper;

    private ToListUniNeighborhoodsCollector(UniNeighborhoodsMapper<Solution_, A, Mapped_> mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public static <Solution_, A, Mapped_> ToListUniNeighborhoodsCollector<Solution_, A, Mapped_>
            create(UniNeighborhoodsMapper<Solution_, A, Mapped_> mapper) {
        return new ToListUniNeighborhoodsCollector<>(mapper);
    }

    @Override
    public Supplier<AbstractToListSlot.State<Mapped_>> supplier() {
        return AbstractToListSlot.State::new;
    }

    @Override
    public UniNeighborhoodsCollectorAccumulator<Solution_, A, AbstractToListSlot.State<Mapped_>> accumulator() {
        return (view, state) -> new Slot(state, view);
    }

    @Override
    public Function<AbstractToListSlot.State<Mapped_>, @Nullable List<Mapped_>> finisher() {
        return AbstractToListSlot.State::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        return object instanceof ToListUniNeighborhoodsCollector<?, ?, ?> other
                && Objects.equals(mapper, other.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }

    private final class Slot
            extends AbstractToListSlot<Mapped_>
            implements UniNeighborhoodsCollectorValueHandle<A> {

        private final SolutionView<Solution_> view;

        Slot(State<Mapped_> state, SolutionView<Solution_> view) {
            super(state);
            this.view = view;
        }

        @Override
        public void add(@Nullable A a) {
            addMapped(mapper.apply(view, a));
        }

        @Override
        public void replaceWith(@Nullable A a) {
            replaceWithMapped(mapper.apply(view, a));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
