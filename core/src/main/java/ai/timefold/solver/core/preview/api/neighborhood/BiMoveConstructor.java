package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@FunctionalInterface
public non-sealed interface BiMoveConstructor<Solution_, A, B>
        extends MoveConstructor {

    Move<Solution_> apply(SolutionView<Solution_> solutionView, @Nullable A a, @Nullable B b);

}
