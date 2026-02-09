package ai.timefold.solver.core.preview.api.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveTestContext;

import org.jspecify.annotations.NullMarked;

/**
 * Provides methods for enumerating moves for a given {@link MoveProvider}
 * using a bound planning solution instance.
 * <p>
 * Created via {@link NeighborhoodTester#using(Object)}, this context binds a specific solution
 * instance to the evaluator and exposes extraction methods.
 * Once any particular move was retrieved using methods such as {@link #getMovesAsStream()},
 * it can optionally be executed using the {@link MoveTestContext} obtained via {@link #getMoveTestContext()}.
 * It is recommended to only {@link MoveTestContext#executeTemporarily(Move, Consumer) execute moves temporarily},
 * as that will have no lasting impact on the iterator or the bound solution instance.
 * <p>
 * This class is NOT thread-safe.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility; any method or field may change
 * or be removed without prior notice, although we will strive to avoid this as much as possible.
 *
 * @param <Solution_> the planning solution type
 */
@NullMarked
public interface NeighborhoodTestContext<Solution_> {

    /**
     * Returns an iterator over all moves provided by the given {@link MoveProvider}
     * for the bound solution instance.
     * The order of moves is not defined and is driven by the underlying {@link MoveProvider},
     * but it is guaranteed to be deterministic and reproducible across multiple invocations.
     *
     * @return an iterator over all moves
     */
    default Iterator<Move<Solution_>> getMovesAsIterator() {
        return getMovesAsIterator(Function.identity());
    }

    /**
     * As defined by {@link #getMovesAsIterator()},
     * but returns a {@link Stream} of all the moves in the iterator.
     * Parallel streams are not supported and the behavior of such stream is undefined.
     *
     * @return a stream of all moves
     */
    default Stream<Move<Solution_>> getMovesAsStream() {
        var iterator = getMovesAsIterator();
        Iterable<Move<Solution_>> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * As defined by {@link #getMovesAsIterator()}, but returns a list of all the moves in the iterator.
     * In case of a large expected number of moves,
     * consider using {@link #getMovesAsIterator()} instead to prevent excessive memory use.
     */
    default List<Move<Solution_>> getMovesAsList() {
        return getMovesAsList(Function.identity());
    }

    /**
     * As defined by {@link #getMovesAsIterator()},
     * but the provided function allows casting each move to a more specific subtype,
     * avoiding the need for external casting in the test.
     * Only applicable if the underlying {@link MoveProvider} only provides moves of one particular subtype.
     *
     * @param moveCaster function to cast each move to the expected subtype
     * @return an iterator over all moves
     * @param <Move_> expected move subtype
     */
    <Move_ extends Move<Solution_>> Iterator<Move_> getMovesAsIterator(Function<Move<Solution_>, Move_> moveCaster);

    /**
     * As defined by {@link #getMovesAsStream()},
     * but the provided function allows casting each move to a more specific subtype,
     * avoiding the need for external casting in the test.
     * Only applicable if the underlying {@link MoveProvider} only provides moves of one particular subtype.
     *
     * @param moveCaster function to cast each move to the expected subtype
     * @return a stream of all moves
     * @param <Move_> expected move subtype
     */
    default <Move_ extends Move<Solution_>> Stream<Move_> getMovesAsStream(Function<Move<Solution_>, Move_> moveCaster) {
        var iterator = getMovesAsIterator(moveCaster);
        Iterable<Move_> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * As defined by {@link #getMovesAsList()},
     * but the provided function allows casting each move to a more specific subtype,
     * avoiding the need for external casting in the test.
     * Only applicable if the underlying {@link MoveProvider} only provides moves of one particular subtype.
     *
     * @param moveCaster function to cast each move to the expected subtype
     * @return a list of all moves
     * @param <Move_> expected move subtype
     */
    default <Move_ extends Move<Solution_>> List<Move_> getMovesAsList(Function<Move<Solution_>, Move_> moveCaster) {
        var moveIterator = getMovesAsIterator(moveCaster);
        var result = new ArrayList<Move_>();
        while (moveIterator.hasNext()) {
            result.add(moveIterator.next());
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    /**
     * May be used to execute moves retrieved from this context on the bound solution instance.
     * When executing moves {@link MoveTestContext#execute(Move) permanently},
     * any non-exhausted iterators obtained from this context become invalid and their further behavior is undefined;
     * new moves should be obtained instead,
     * for example by calling {@link #getMovesAsStream()}.
     * Temporary execution via {@link MoveTestContext#executeTemporarily(Move, Consumer)} does not have this limitation.
     * Java streams and lists obtained from this context are not affected by permanent execution of moves either.
     * Move lists are based on the solution as it was at the time when they were obtained.
     * Streams of moves only materialize when their terminal operation is invoked,
     * and they reflect the latest state of the solution at that time.
     *
     * @return the move run context for executing moves on the bound solution instance
     */
    MoveTestContext<Solution_> getMoveTestContext();

}
