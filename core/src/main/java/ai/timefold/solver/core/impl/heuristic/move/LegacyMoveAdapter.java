package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;

import ai.timefold.solver.core.api.move.Move;
import ai.timefold.solver.core.api.move.MutableSolutionState;
import ai.timefold.solver.core.api.move.SolutionState;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionState;

record LegacyMoveAdapter<Solution_>(
        ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> legacyMove) implements Move<Solution_> {

    @Override
    public void run(MutableSolutionState<Solution_> mutableSolutionState) {
        legacyMove.doMoveOnly(getScoreDirector(mutableSolutionState));
    }

    private ScoreDirector<Solution_> getScoreDirector(SolutionState<Solution_> mutableSolutionState) {
        return ((InnerMutableSolutionState<Solution_>) mutableSolutionState).getVariableChangeRecordingScoreDirector();
    }

    @Override
    public Move<Solution_> rebase(SolutionState<Solution_> solutionState) {
        return new LegacyMoveAdapter<>(legacyMove.rebase(getScoreDirector(solutionState)));
    }

    @Override
    public Collection<?> getPlanningEntities() {
        return legacyMove.getPlanningEntities();
    }

    @Override
    public Collection<?> getPlanningValues() {
        return legacyMove.getPlanningValues();
    }

}
