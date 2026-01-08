package ai.timefold.solver.core.preview.api.neighborhood.function;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.BiEnumeratingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A filter that can be applied to a {@link BiEnumeratingStream} to filter out pairs of data,
 * optionally using {@link SolutionView} to query for solution state.
 *
 * @param <Solution_> the type of the solution
 * @param <A> the type of the first parameter
 * @param <B> the type of the second parameter
 */
@NullMarked
public interface BiNeighborhoodsPredicate<Solution_, A, B> extends TriPredicate<SolutionView<Solution_>, A, B> {

    @Override
    boolean test(SolutionView<Solution_> solutionView, @Nullable A a, @Nullable B b);

    @Override
    default BiNeighborhoodsPredicate<Solution_, A, B>
            and(TriPredicate<? super SolutionView<Solution_>, ? super A, ? super B> other) {
        return (solutionView, a, b) -> test(solutionView, a, b)
                && other.test(solutionView, a, b);
    }

    default BiPredicate<A, B> toBiPredicate(SolutionView<Solution_> solutionView) {
        return (a, b) -> test(solutionView, a, b);
    }

}
