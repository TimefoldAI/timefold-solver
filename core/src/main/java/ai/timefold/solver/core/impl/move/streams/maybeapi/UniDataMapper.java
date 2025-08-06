package ai.timefold.solver.core.impl.move.streams.maybeapi;

import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A mapping function that can be applied to {@link UniDataStream} to transform data,
 * optionally using {@link SolutionView} to query for solution state.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the first parameter
 */
@NullMarked
public interface UniDataMapper<Solution_, A, Result_> extends BiFunction<SolutionView<Solution_>, A, Result_> {

    @Override
    Result_ apply(SolutionView<Solution_> solutionSolutionView, @Nullable A a);

    default Function<A, Result_> toFunction(SolutionView<Solution_> solutionView) {
        return a -> apply(solutionView, a);
    }

}
