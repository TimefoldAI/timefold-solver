package ai.timefold.solver.core.preview.api.neighborhood.stream.collector;

import java.util.List;

import ai.timefold.solver.core.impl.neighborhood.stream.collector.ToListBiNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.collector.ToListUniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;

/**
 * Factory for {@link UniNeighborhoodsCollector} and {@link BiNeighborhoodsCollector} instances.
 */
@NullMarked
public final class NeighborhoodsCollectors {

    private NeighborhoodsCollectors() {
    }

    /**
     * Collects all facts from a {@link UniEnumeratingStream} group into a {@link List}.
     */
    public static <Solution_, A> UniNeighborhoodsCollector<Solution_, A, ?, List<A>> toList() {
        return ToListUniNeighborhoodsCollector.create();
    }

    /**
     * Collects all facts from a {@link BiEnumeratingStream} group into a {@link List},
     * applying the given mapping function to each {@code (A, B)} pair before collecting.
     * The mapping function also has access to the working solution via {@link BiNeighborhoodsMapper}.
     *
     * @param mapper maps each tuple to a single value to collect
     */
    public static <Solution_, A, B, Mapped_> BiNeighborhoodsCollector<Solution_, A, B, ?, List<Mapped_>> toList(
            BiNeighborhoodsMapper<Solution_, A, B, Mapped_> mapper) {
        return ToListBiNeighborhoodsCollector.create(mapper);
    }

}
