package ai.timefold.solver.core.preview.api.neighborhood;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EvaluatedNeighborhood<Solution_> {

    default Iterator<Move<Solution_>> getMoveIterator() {
        return getMoveIterator(Function.identity());
    }

    default List<Move<Solution_>> getMoves() {
        return getMoves(Function.identity());
    }

    default <Move_ extends Move<Solution_>> List<Move_> getMoves(Function<Move<Solution_>, Move_> moveCaster) {
        var moveIterator = getMoveIterator(moveCaster);
        var result = new ArrayList<Move_>();
        while (moveIterator.hasNext()) {
            result.add(moveIterator.next());
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    <Move_ extends Move<Solution_>> Iterator<Move_> getMoveIterator(Function<Move<Solution_>, Move_> moveCaster);

    MoveRunContext<Solution_> getMoveRunContext();

}
