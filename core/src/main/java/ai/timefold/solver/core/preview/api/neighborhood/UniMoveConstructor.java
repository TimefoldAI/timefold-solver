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
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
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
