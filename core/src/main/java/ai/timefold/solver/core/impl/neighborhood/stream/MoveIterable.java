package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface MoveIterable<Solution_> extends Iterable<Move<Solution_>> {

    Iterator<Move<Solution_>> iterator(RandomGenerator random);

    /**
     * Move order is never part of the Neighborhoods API's contract; only {@link #iterator(RandomGenerator)} is
     * supported. This override exists solely so that {@link MoveIterable} remains a subtype of
     * {@code Iterable<Move<Solution_>>}, as required by
     * {@link ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream#getMoveIterable}.
     */
    @Override
    default Iterator<Move<Solution_>> iterator() {
        throw new UnsupportedOperationException(
                "Move order is not part of the Neighborhoods API's contract. Use iterator(RandomGenerator) instead.");
    }

}
