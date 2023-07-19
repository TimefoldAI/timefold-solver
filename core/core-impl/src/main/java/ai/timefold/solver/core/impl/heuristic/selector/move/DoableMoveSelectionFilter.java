package ai.timefold.solver.core.impl.heuristic.selector.move;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;

final class DoableMoveSelectionFilter<Solution_> implements SelectionFilter<Solution_, Move<Solution_>> {

    static final SelectionFilter INSTANCE = new DoableMoveSelectionFilter<>();

    @Override
    public boolean accept(ScoreDirector<Solution_> scoreDirector, Move<Solution_> move) {
        return move.isMoveDoable(scoreDirector);
    }

    private DoableMoveSelectionFilter() {

    }

    @Override
    public String toString() {
        return "Doable moves only";
    }
}
