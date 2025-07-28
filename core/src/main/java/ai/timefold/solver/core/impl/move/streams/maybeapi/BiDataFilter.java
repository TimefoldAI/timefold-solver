package ai.timefold.solver.core.impl.move.streams.maybeapi;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface BiDataFilter<Solution_, A, B> extends TriPredicate<SolutionView<Solution_>, A, B> {

    @Override
    boolean test(SolutionView<Solution_> solutionView, @Nullable A a, @Nullable B b);

    @Override
    default BiDataFilter<Solution_, A, B> and(TriPredicate<? super SolutionView<Solution_>, ? super A, ? super B> other) {
        return (BiDataFilter<Solution_, A, B>) TriPredicate.super.and(other);
    }

    default BiPredicate<A, B> toBiPredicate(SolutionView<Solution_> solutionView) {
        return (a, b) -> test(solutionView, a, b);
    }

}
