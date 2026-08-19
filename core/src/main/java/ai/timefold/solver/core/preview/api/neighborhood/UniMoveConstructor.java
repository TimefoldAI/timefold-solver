package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.UniSamplingStream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A functional interface for constructing a {@link Move} from a single picked element.
 * <p>
 * Use this with {@link UniSamplingStream#asMove(UniMoveConstructor)}
 * to build moves from a single pick, without requiring a second pick.
 *
 * @param <Solution_> the solution type
 * @param <A> the type of the picked element
 */
@NullMarked
@FunctionalInterface
public non-sealed interface UniMoveConstructor<Solution_, A>
        extends MoveConstructor {

    Move<Solution_> apply(SolutionView<Solution_> solutionView, @Nullable A a);

}
