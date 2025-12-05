package ai.timefold.solver.core.impl.heuristic.move;

import java.util.Collection;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.move.InnerMutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.MutableSolutionView;
import ai.timefold.solver.core.preview.api.move.Rebaser;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;

/**
 * Adapts {@link ai.timefold.solver.core.impl.heuristic.move.Move a legacy move}
 * to {@link Move a new move}.
 * Once the move selector framework is removed, this may be removed as well.
 * 
 * @param legacyMove the move to adapt
 * @param <Solution_>
 */
@NullMarked
record LegacyMoveAdapter<Solution_>(ai.timefold.solver.core.impl.heuristic.move.Move<Solution_> legacyMove)
        implements
            Move<Solution_> {

    @Override
    public void execute(MutableSolutionView<Solution_> solutionView) {
        var scoreDirector = getScoreDirector(solutionView);
        legacyMove.doMoveOnly(scoreDirector);
    }

    private ScoreDirector<Solution_> getScoreDirector(SolutionView<Solution_> solutionView) {
        return ((InnerMutableSolutionView<Solution_>) solutionView).getScoreDirector();
    }

    @SuppressWarnings("unchecked")
    private ScoreDirector<Solution_> getScoreDirector(Rebaser rebaser) {
        return ((InnerMutableSolutionView<Solution_>) rebaser).getScoreDirector();
    }

    public boolean isMoveDoable(SolutionView<Solution_> solutionView) {
        return legacyMove.isMoveDoable(getScoreDirector(solutionView));
    }

    @Override
    public String describe() {
        return legacyMove.getSimpleMoveTypeDescription();
    }

    @Override
    public Move<Solution_> rebase(Rebaser rebaser) {
        return MoveAdapters.toNewMove(legacyMove.rebase(getScoreDirector(rebaser)));
    }

    @Override
    public Collection<?> extractPlanningEntities() {
        return legacyMove.getPlanningEntities();
    }

    @Override
    public Collection<?> extractPlanningValues() {
        return legacyMove.getPlanningValues();
    }

    @Override
    public String toString() {
        return legacyMove.toString();
    }
}
