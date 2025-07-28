package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.impl.move.streams.maybeapi.BiDataFilter;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiDataStream<Solution_, A, B> extends DataStream<Solution_> {

    /**
     * Exhaustively test each fact against the {@link BiDataFilter}
     * and match if {@link BiDataFilter#test(SolutionView, Object, Object)} returns true.
     */
    BiDataStream<Solution_, A, B> filter(BiDataFilter<Solution_, A, B> filter);

}
