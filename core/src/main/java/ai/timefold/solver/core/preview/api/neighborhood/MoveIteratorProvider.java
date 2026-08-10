package ai.timefold.solver.core.preview.api.neighborhood;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * Builds a custom {@link Iterator} of {@link Move}s from datasets registered via
 * {@link MoveStreamFactory#register}, for use with {@link MoveStreamFactory#buildMoveStream(MoveIteratorProvider)}.
 * <p>
 * The order in which the returned iterator yields moves is never part of the API contract,
 * and must never be asserted on or relied upon; only which moves are producible is API surface.
 * For this reason, this interface only ever produces a single, random-order iterator;
 * there is no original/deterministic-order variant.
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
 */
@NullMarked
@FunctionalInterface
public interface MoveIteratorProvider<Solution_> {

    Iterator<Move<Solution_>> iterator(MoveIteratorSession<Solution_> session, RandomGenerator random);

}
