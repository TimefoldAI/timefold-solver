package ai.timefold.solver.core.preview.api.neighborhood;

import java.util.Iterator;

import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MoveRunner;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodEvaluationContext<Solution_> {

    <Move_ extends Move<Solution_>> Iterator<Move_> evaluate(Class<MoveProvider<Solution_>> moveProviderClass);

    MoveRunner<Solution_> getMoveRunner();

}
