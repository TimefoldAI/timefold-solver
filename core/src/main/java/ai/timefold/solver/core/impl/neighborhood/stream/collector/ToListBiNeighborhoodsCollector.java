package ai.timefold.solver.core.impl.neighborhood.stream.collector;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.score.stream.collector.AbstractToListSlot;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.BiNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.BiNeighborhoodsCollectorAccumulator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.BiNeighborhoodsCollectorValueHandle;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ToListBiNeighborhoodsCollector<Solution_, A, B, Mapped_>
        implements BiNeighborhoodsCollector<Solution_, A, B, AbstractToListSlot.State<Mapped_>, List<Mapped_>> {

    private final BiNeighborhoodsMapper<Solution_, A, B, Mapped_> mapper;

    private ToListBiNeighborhoodsCollector(BiNeighborhoodsMapper<Solution_, A, B, Mapped_> mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public static <Solution_, A, B, Mapped_> ToListBiNeighborhoodsCollector<Solution_, A, B, Mapped_>
            create(BiNeighborhoodsMapper<Solution_, A, B, Mapped_> mapper) {
        return new ToListBiNeighborhoodsCollector<>(mapper);
    }

    @Override
    public Supplier<AbstractToListSlot.State<Mapped_>> supplier() {
        return AbstractToListSlot.State::new;
    }

    @Override
    public BiNeighborhoodsCollectorAccumulator<Solution_, A, B, AbstractToListSlot.State<Mapped_>> accumulator() {
        return (view, state) -> new Slot<>(state, mapper, view);
    }

    @Override
    public Function<AbstractToListSlot.State<Mapped_>, @Nullable List<Mapped_>> finisher() {
        return AbstractToListSlot.State::result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object)
            return true;
        if (object == null || getClass() != object.getClass())
            return false;
        var that = (ToListBiNeighborhoodsCollector<?, ?, ?, ?>) object;
        return Objects.equals(mapper, that.mapper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapper);
    }

    private static final class Slot<Solution_, A, B, Mapped_> extends AbstractToListSlot<Mapped_>
            implements BiNeighborhoodsCollectorValueHandle<A, B> {

        private final BiNeighborhoodsMapper<Solution_, A, B, Mapped_> mapper;
        private final SolutionView<Solution_> view;

        Slot(AbstractToListSlot.State<Mapped_> state, BiNeighborhoodsMapper<Solution_, A, B, Mapped_> mapper,
                SolutionView<Solution_> view) {
            super(state);
            this.mapper = mapper;
            this.view = view;
        }

        @Override
        public void add(@Nullable A a, @Nullable B b) {
            addMapped(mapper.apply(view, a, b));
        }

        @Override
        public void replaceWith(@Nullable A a, @Nullable B b) {
            replaceWithMapped(mapper.apply(view, a, b));
        }

        @Override
        public void remove() {
            removeMapped();
        }
    }
}
