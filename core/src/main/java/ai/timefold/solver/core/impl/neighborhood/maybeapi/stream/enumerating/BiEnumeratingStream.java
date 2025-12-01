package ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingMapper;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.BiEnumeratingPredicate;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.enumerating.function.UniEnumeratingMapper;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiEnumeratingStream<Solution_, A, B> extends EnumeratingStream {

    /**
     * Exhaustively test each fact against the {@link BiEnumeratingPredicate}
     * and match if {@link BiEnumeratingPredicate#test(SolutionView, Object, Object)} returns true.
     */
    BiEnumeratingStream<Solution_, A, B> filter(BiEnumeratingPredicate<Solution_, A, B> filter);

    // ************************************************************************
    // Operations with duplicate tuple possibility
    // ************************************************************************

    /**
     * As defined by {@link UniEnumeratingStream#map(UniEnumeratingMapper)}.
     *
     * <p>
     * Use with caution,
     * as the increased memory allocation rates coming from tuple creation may negatively affect performance.
     *
     * @param mapping function to convert the original tuple into the new tuple
     * @param <ResultA_> the type of the only fact in the resulting {@link UniEnumeratingStream}'s tuple
     */
    <ResultA_> UniEnumeratingStream<Solution_, ResultA_> map(BiEnumeratingMapper<Solution_, A, B, ResultA_> mapping);

    /**
     * As defined by {@link #map(BiEnumeratingMapper)}, only resulting in {@link BiEnumeratingStream}.
     *
     * @param mappingA function to convert the original tuple into the first fact of a new tuple
     * @param mappingB function to convert the original tuple into the second fact of a new tuple
     * @param <ResultA_> the type of the first fact in the resulting {@link BiEnumeratingStream}'s tuple
     * @param <ResultB_> the type of the first fact in the resulting {@link BiEnumeratingStream}'s tuple
     */
    <ResultA_, ResultB_> BiEnumeratingStream<Solution_, ResultA_, ResultB_> map(
            BiEnumeratingMapper<Solution_, A, B, ResultA_> mappingA,
            BiEnumeratingMapper<Solution_, A, B, ResultB_> mappingB);

    /**
     * As defined by {@link UniEnumeratingStream#distinct()}.
     */
    BiEnumeratingStream<Solution_, A, B> distinct();

}
