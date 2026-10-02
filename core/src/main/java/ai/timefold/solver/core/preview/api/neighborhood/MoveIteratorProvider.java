package ai.timefold.solver.core.preview.api.neighborhood;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;

import org.jspecify.annotations.NullMarked;

/**
 * Builds a custom {@link Iterator} of {@link Move}s from datasets cached via
 * {@code asCachedDataset()}, for use with {@link MoveStreamFactory#buildMoveStream(MoveIteratorProvider)}.
 * <p>
 * The order in which the returned iterator yields moves is never part of the API contract,
 * and must never be asserted on or relied upon; only which moves are producible is API surface.
 * For this reason, this interface only ever produces a single, random-order iterator;
 * there is no original/deterministic-order variant.
 *
 * @param <Solution_> the solution type
 */
@NullMarked
@FunctionalInterface
public interface MoveIteratorProvider<Solution_> {

    Iterator<Move<Solution_>> iterator(MoveIteratorSession<Solution_> session, RandomGenerator random);

}
