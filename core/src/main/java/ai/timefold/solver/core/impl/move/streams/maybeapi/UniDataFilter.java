package ai.timefold.solver.core.impl.move.streams.maybeapi;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.UniDataStream;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A filter that can be applied to a {@link UniDataStream} to filter out pairs of data,
 * optionally using {@link SolutionView} to query for solution state.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the first parameter
 */
@NullMarked
public interface UniDataFilter<Solution_, A> extends BiPredicate<SolutionView<Solution_>, A> {

    @Override
    boolean test(SolutionView<Solution_> solutionView, @Nullable A a);

    @Override
    default UniDataFilter<Solution_, A> and(BiPredicate<? super SolutionView<Solution_>, ? super A> other) {
        return (UniDataFilter<Solution_, A>) BiPredicate.super.and(other);
    }

    default Predicate<A> toPredicate(SolutionView<Solution_> solutionView) {
        return a -> test(solutionView, a);
    }

}
