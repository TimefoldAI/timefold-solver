package ai.timefold.solver.core.preview.api.neighborhood.stream;

import java.util.Iterator;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
@FunctionalInterface
public interface MoveIterable<Solution_> {

    Iterator<Move<Solution_>> iterator(RandomGenerator random);

}
