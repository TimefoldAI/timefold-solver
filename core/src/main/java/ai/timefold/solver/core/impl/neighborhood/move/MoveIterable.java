package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveIterable<Solution_> extends Iterable<Move<Solution_>> {

    Iterator<Move<Solution_>> iterator(Random random);

}
