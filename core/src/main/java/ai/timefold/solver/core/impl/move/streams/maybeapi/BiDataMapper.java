package ai.timefold.solver.core.impl.move.streams.maybeapi;

import java.util.function.BiFunction;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.BiDataStream;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A mapping function that can be applied to {@link BiDataStream} to transform data,
 * optionally using {@link SolutionView} to query for solution state.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the first parameter
 * @param <B> the type of the second parameter
 */
@NullMarked
public interface BiDataMapper<Solution_, A, B, Result_> extends TriFunction<SolutionView<Solution_>, A, B, Result_> {

    @Override
    Result_ apply(SolutionView<Solution_> solutionSolutionView, @Nullable A a, @Nullable B b);

    default BiFunction<A, B, Result_> toBiFunction(SolutionView<Solution_> solutionView) {
        return (a, b) -> apply(solutionView, a, b);
    }

}
