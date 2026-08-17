package ai.timefold.solver.core.preview.api.neighborhood.test;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;

import org.jspecify.annotations.NullMarked;

/**
 * Membership-based assertions over the moves a {@link MoveProvider} can produce for a bound solution instance.
 * Because move order is never part of the Neighborhoods API's contract,
 * only which moves are producible is assertable;
 * never an exact sequence.
 * <p>
 * Both assertions repeatedly draw moves (up to an iteration limit) and compare them to the expected moves via
 * {@link Object#equals(Object) equals()}:
 * {@link #producesAllOf} fails if the limit is exhausted while any expected move has not been seen;
 * {@link #producesNoneOf} fails as soon as any expected move is seen.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface NeighborhoodMoveAsserter<Solution_> {

    /**
     * Returns a new asserter configured with an explicit iteration limit,
     * overriding the default;
     * this asserter is left unmodified.
     */
    NeighborhoodMoveAsserter<Solution_> within(int iterationLimit);

    /**
     * Asserts that every move in the provided array of expected moves can be
     * produced by a {@link MoveProvider} for a bound solution instance.
     * The method repeatedly generates moves (subject to an iteration limit)
     * and checks if all expected moves are produced.
     * If the iteration limit is reached and any expected move has not been observed,
     * the assertion fails.
     *
     * @param expectedMoves the array of moves that are expected to be
     *        producible; each move is verified using {@link Object#equals(Object)}
     *        to check for a match
     */
    @SuppressWarnings("unchecked")
    void producesAllOf(Move<Solution_>... expectedMoves);

    /**
     * Asserts that none of the moves in the provided array of expected moves
     * can be produced by a {@link MoveProvider} for a bound solution instance.
     * The method repeatedly generates moves (subject to an iteration limit)
     * and checks for the existence of the expected moves.
     * If any expected move is observed during the generation process,
     * the assertion fails immediately.
     *
     * @param unexpectedMoves the array of moves that are expected to not be
     *        producible; each move is verified using {@link Object#equals(Object)}
     *        to check for a match
     */
    @SuppressWarnings("unchecked")
    void producesNoneOf(Move<Solution_>... unexpectedMoves);

}
