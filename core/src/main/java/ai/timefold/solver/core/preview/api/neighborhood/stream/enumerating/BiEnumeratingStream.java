package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiEnumeratingStream<Solution_, A, B> extends EnumeratingStream {

    /**
     * Exhaustively test each fact against the {@link BiNeighborhoodsPredicate}
     * and match if {@link BiNeighborhoodsPredicate#test(SolutionView, Object, Object)} returns true.
     */
    BiEnumeratingStream<Solution_, A, B> filter(BiNeighborhoodsPredicate<Solution_, A, B> filter);

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    /**
     * As defined by {@link UniEnumeratingStream#map(UniNeighborhoodsMapper)}.
     *
     * <p>
     * Use with caution,
     * as the increased memory allocation rates coming from tuple creation may negatively affect performance.
     *
     * @param mapping function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniEnumeratingStream}'s tuple
     */
    <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(BiNeighborhoodsMapper<Solution_, A, B, ResultA_> mapping);

    /**
     * As defined by {@link #map(BiNeighborhoodsMapper)}, only resulting in {@link BiEnumeratingStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiEnumeratingStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiEnumeratingStream}'s tuple
     */
    <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_> map(
            BiNeighborhoodsMapper<Solution_, A, B, ResultA_> mappingA,
            BiNeighborhoodsMapper<Solution_, A, B, ResultB_> mappingB);

    /**
     * As defined by {@link UniEnumeratingStream#distinct()}.
     */
    BiEnumeratingStream<Solution_, A, B> distinct();

}
