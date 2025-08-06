package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataMapper;
import ai.timefold.solver.core.impl.move.streams.maybeapi.UniDataMapper;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiDataStream<Solution_, A, B> extends DataStream<Solution_> {

    /**
     * Exhaustively test each fact against the {@link BiDataFilter}
     * and match if {@link BiDataFilter#test(SolutionView, Object, Object)} returns true.
     */
    BiDataStream<Solution_, A, B> filter(BiDataFilter<Solution_, A, B> filter);

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    /**
     * As defined by {@link UniDataStream#map(UniDataMapper)}.
     *
     * <p>
     * Use with caution,
     * as the increased memory allocation rates coming from tuple creation may negatively affect performance.
     *
     * @param mapping function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniDataStream}'s tuple
     */
    <ResultA_> UniDataStream<Solution_, ResultA_> map(BiDataMapper<Solution_, A, B, ResultA_> mapping);

    /**
     * As defined by {@link #map(BiDataMapper)}, only resulting in {@link BiDataStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiDataStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiDataStream}'s tuple
     */
    <ResultA_, ResultB_> BiDataStream<Solution_, ResultA_, ResultB_> map(BiDataMapper<Solution_, A, B, ResultA_> mappingA,
            BiDataMapper<Solution_, A, B, ResultB_> mappingB);

    /**
     * As defined by {@link UniDataStream#distinct()}.
     */
    BiDataStream<Solution_, A, B> distinct();

}
