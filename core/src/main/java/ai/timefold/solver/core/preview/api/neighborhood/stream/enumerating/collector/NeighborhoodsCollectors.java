package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.AndThenBiNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.AndThenUniNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.ComposeTwoBiNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.ComposeTwoUniNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.ToListBiNeighborhoodsCollector;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.collector.ToListUniNeighborhoodsCollector;
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

    /**
     * Collects results from a {@link UniNeighborhoodsCollector} and maps its result to another value.
     * <p>
     * This is a better performing alternative to {@code .groupBy(...).map(...)}.
     *
     * @param <Solution_> the type of the solution
     * @param <A> generic type of the tuple variable
     * @param <Intermediate_> generic type of the delegate's return value
     * @param <Result_> generic type of the final collector's return value
     * @param delegate the underlying collector to delegate to
     * @param mappingFunction maps the result of the underlying collector to another value
     */
    public static <Solution_, A, Intermediate_, Result_>
            UniNeighborhoodsCollector<Solution_, A, ?, Result_>
            collectAndThen(UniNeighborhoodsCollector<Solution_, A, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return new AndThenUniNeighborhoodsCollector<>(delegate, mappingFunction);
    }

    /**
     * As defined by {@link #collectAndThen(UniNeighborhoodsCollector, Function)}.
     */
    public static <Solution_, A, B, Intermediate_, Result_>
            BiNeighborhoodsCollector<Solution_, A, B, ?, Result_>
            collectAndThen(BiNeighborhoodsCollector<Solution_, A, B, ?, Intermediate_> delegate,
                    Function<Intermediate_, Result_> mappingFunction) {
        return new AndThenBiNeighborhoodsCollector<>(delegate, mappingFunction);
    }

    /**
     * Composes two {@link UniNeighborhoodsCollector}s into one, combining their results with the given function.
     */
    public static <Solution_, A, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            UniNeighborhoodsCollector<Solution_, A, ?, Result_> compose(
                    UniNeighborhoodsCollector<Solution_, A, ResultHolder1_, Result1_> first,
                    UniNeighborhoodsCollector<Solution_, A, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoUniNeighborhoodsCollector<>(first, second, composeFunction);
    }

    /**
     * Composes two {@link BiNeighborhoodsCollector}s into one, combining their results with the given function.
     */
    public static <Solution_, A, B, ResultHolder1_, ResultHolder2_, Result1_, Result2_, Result_>
            BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> compose(
                    BiNeighborhoodsCollector<Solution_, A, B, ResultHolder1_, Result1_> first,
                    BiNeighborhoodsCollector<Solution_, A, B, ResultHolder2_, Result2_> second,
                    BiFunction<Result1_, Result2_, Result_> composeFunction) {
        return new ComposeTwoBiNeighborhoodsCollector<>(first, second, composeFunction);
    }

}
