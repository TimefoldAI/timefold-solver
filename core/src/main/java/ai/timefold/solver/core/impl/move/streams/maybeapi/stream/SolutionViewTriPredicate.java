package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.api.function.TriPredicate;
import ai.timefold.solver.core.preview.api.move.SolutionView;

@FunctionalInterface
public interface SolutionViewTriPredicate<Solution_, A, B>
        extends TriPredicate<SolutionView<Solution_>, A, B> {

    @SuppressWarnings("rawtypes")
    SolutionViewTriPredicate TRUE = (solutionView, a, b) -> true;

    default SolutionViewTriPredicate<Solution_, A, B> and(SolutionViewTriPredicate<Solution_, A, B> other) {
        return (solutionView, a, b) -> this.test(solutionView, a, b) && other.test(solutionView, a, b);
    }

}
