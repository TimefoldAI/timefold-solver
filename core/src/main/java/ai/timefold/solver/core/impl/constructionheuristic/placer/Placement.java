package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.Iterator;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.heuristic.move.Move;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class Placement<Solution_> implements Iterable<Move<Solution_>> {

    private final Iterator<Move<Solution_>> moveIterator;

    public Placement(Iterator<Move<Solution_>> moveIterator) {
        this.moveIterator = moveIterator;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return moveIterator;
    }

    @Override
    public String toString() {
        return "Placement (" + moveIterator + ")";
    }

}
