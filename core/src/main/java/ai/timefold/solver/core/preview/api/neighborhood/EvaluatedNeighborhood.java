package ai.timefold.solver.core.preview.api.neighborhood;

import java.util.Iterator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunContext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface EvaluatedNeighborhood<Solution_> {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    default Iterator<Move<Solution_>> getMoveIterator() {
        return (Iterator) getMoveIterator(Move.class);
    }

    <Move_ extends Move<Solution_>> Iterator<Move_> getMoveIterator(Class<Move_> moveClass);

    MoveRunContext<Solution_> getMoveRunContext();

}
