package ai.timefold.solver.core.impl.move.streams;

import java.util.Iterator;
import java.util.Random;

import ai.timefold.solver.core.preview.api.move.Move;

public interface MoveIterable<Solution_> extends Iterable<Move<Solution_>> {

    Iterator<Move<Solution_>> iterator(Random random);

}
