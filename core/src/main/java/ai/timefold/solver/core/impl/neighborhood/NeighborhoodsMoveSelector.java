package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.MoveAdapters;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class NeighborhoodsMoveSelector<Solution_> extends AbstractMoveSelector<Solution_> {

    private final NeighborhoodsBasedMoveRepository<Solution_> moveRepository;

    public NeighborhoodsMoveSelector(NeighborhoodsBasedMoveRepository<Solution_> moveRepository) {
        this.moveRepository = moveRepository;
    }

    @Override
    public long getSize() {
        throw new UnsupportedOperationException("Neighborhood size is not supported by the Neighborhoods API.");
    }

    @Override
    public boolean isCountable() {
        return true;
    }

    @Override
    public boolean isNeverEnding() {
        return moveRepository.isNeverEnding();
    }

    @Override
    public SelectionCacheType getCacheType() {
        return SelectionCacheType.PHASE;
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        moveRepository.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        moveRepository.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        moveRepository.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        moveRepository.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        moveRepository.phaseEnded(phaseScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        moveRepository.solvingEnded(solverScope);
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return MoveAdapters.toLegacyMove(moveRepository.iterator());
    }
}
