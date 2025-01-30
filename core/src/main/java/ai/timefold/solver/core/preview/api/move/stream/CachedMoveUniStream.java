package ai.timefold.solver.core.preview.api.move.stream;

import java.util.function.Function;

public interface CachedMoveUniStream<Solution_, A> {

    CachedMoveUniStream<Solution_, A> filter(Function<A, Boolean> filter);

    CachedMoveUniStream<Solution_, A> addNull();

}
