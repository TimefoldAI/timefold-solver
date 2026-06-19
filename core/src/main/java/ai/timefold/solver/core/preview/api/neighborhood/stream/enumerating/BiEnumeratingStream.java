package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.BiNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsPredicate;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiEnumeratingStream<Solution_, A, B> extends EnumeratingStream {

    /**
     * As defined by {@link UniEnumeratingStream#filter(UniNeighborhoodsPredicate)}.
     */
    BiEnumeratingStream<Solution_, A, B> filter(BiNeighborhoodsPredicate<Solution_, A, B> filter);

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    /**
     * As defined by {@link UniEnumeratingStream#map(UniNeighborhoodsMapper)}.
     */
    <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(BiNeighborhoodsMapper<Solution_, A, B, ResultA_> mapping);

    /**
     * As defined by {@link #map(BiNeighborhoodsMapper)}, only resulting in {@link BiEnumeratingStream}.
     */
    <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_> map(
            BiNeighborhoodsMapper<Solution_, A, B, ResultA_> mappingA,
            BiNeighborhoodsMapper<Solution_, A, B, ResultB_> mappingB);

    /**
     * As defined by {@link UniEnumeratingStream#groupBy(UniNeighborhoodsMapper)},
     * only for {@link BiEnumeratingStream} sources.
     */
    <GroupKey_> UniEnumeratingStream<Solution_, GroupKey_> groupBy(BiNeighborhoodsMapper<Solution_, A, B, GroupKey_> key);

    /**
     * As defined by
     * {@link UniEnumeratingStream#groupBy(UniNeighborhoodsCollector)},
     * only for {@link BiEnumeratingStream} sources.
     */
    <Result_> UniEnumeratingStream<Solution_, Result_> groupBy(BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> collector);

    /**
     * As defined by
     * {@link UniEnumeratingStream#groupBy(UniNeighborhoodsMapper, UniNeighborhoodsCollector)},
     * only for {@link BiEnumeratingStream} sources.
     */
    <GroupKey_, Result_> BiEnumeratingStream<Solution_, GroupKey_, Result_> groupBy(
            BiNeighborhoodsMapper<Solution_, A, B, GroupKey_> key,
            BiNeighborhoodsCollector<Solution_, A, B, ?, Result_> collector);

    /**
     * As defined by {@link UniEnumeratingStream#distinct()}.
     */
    BiEnumeratingStream<Solution_, A, B> distinct();

}
