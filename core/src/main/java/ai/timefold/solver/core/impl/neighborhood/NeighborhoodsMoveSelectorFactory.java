package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.move.MoveAdapters;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelectorFactory;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

public final class NeighborhoodsMoveSelectorFactory<Solution_>
        implements MoveSelectorFactory<Solution_> {

    private final NeighborhoodsBasedMoveRepository<Solution_> moveRepository;

    public NeighborhoodsMoveSelectorFactory(NeighborhoodsBasedMoveRepository<Solution_> moveRepository) {
        this.moveRepository = Objects.requireNonNull(moveRepository);
    }

    @Override
    public MoveSelector<Solution_> buildMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder, boolean skipNonDoableMoves) {
        return new NeighborhoodsMoveSelector<>(moveRepository);
    }

    private static final class NeighborhoodsMoveSelector<Solution_> extends AbstractMoveSelector<Solution_> {

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
        public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
            super.phaseStarted(phaseScope);
            phaseScope.getScoreDirector().setMoveRepository(moveRepository);
        }

        @Override
        public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
            super.phaseEnded(phaseScope);
            phaseScope.getScoreDirector().setMoveRepository(null);
        }

        @Override
        public Iterator<Move<Solution_>> iterator() {
            return MoveAdapters.toLegacyMove(moveRepository.iterator());
        }
    }

}
