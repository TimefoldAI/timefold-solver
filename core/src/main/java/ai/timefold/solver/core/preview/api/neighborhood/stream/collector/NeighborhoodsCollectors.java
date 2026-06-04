package ai.timefold.solver.core.preview.api.neighborhood.stream.collector;

import java.util.List;

import ai.timefold.solver.core.impl.neighborhood.stream.collector.ToListBiNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.collector.ToListUniNeighborhoodsCollector;
import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.UniEnumeratingStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;

/**
 * Factory for {@link UniNeighborhoodsCollector} and {@link BiNeighborhoodsCollector} instances.
 */
@NullMarked
public final class NeighborhoodsCollectors {

    private NeighborhoodsCollectors() {
    }

    /**
     * As defined by {@link #toList(UniNeighborhoodsMapper)},
     * but using the fact directly without mapping.
     */
    public static <Solution_, A> UniNeighborhoodsCollector<Solution_, A, ?, List<A>> toList() {
        return toList(ConstantLambdaUtils.neighborhoodsUniPickFirst());
    }

    /**
     * Collects all facts from a {@link UniEnumeratingStream} group into a {@link List},
     * applying the given mapping function to each before collecting.
     *
     * @param mapper maps each tuple to a single value to collect
     */
    public static <Solution_, A, Mapped_> UniNeighborhoodsCollector<Solution_, A, ?, List<Mapped_>>
            toList(UniNeighborhoodsMapper<Solution_, A, Mapped_> mapper) {
        return ToListUniNeighborhoodsCollector.create(mapper);
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
