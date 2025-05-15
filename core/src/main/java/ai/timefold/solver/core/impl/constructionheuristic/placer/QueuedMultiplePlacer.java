package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.CartesianProductMoveSelector;
import ai.timefold.solver.core.impl.move.generic.CompositeMove;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

public class QueuedMultiplePlacer<Solution_> extends AbstractEntityPlacer<Solution_>
        implements EntityPlacer<Solution_> {

    private final List<EntityPlacer<Solution_>> queuedPlacerList;

    public QueuedMultiplePlacer(EntityPlacerFactory<Solution_> factory,
            HeuristicConfigPolicy<Solution_> configPolicy, List<EntityPlacer<Solution_>> queuedPlacerList) {
        super(factory, configPolicy);
        this.queuedPlacerList = queuedPlacerList;
        this.queuedPlacerList.forEach(queuedPlacer -> phaseLifecycleSupport.addEventListener(queuedPlacer));

    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        var filteredQueuedPlacerList = queuedPlacerList.stream()
                .map(placer -> placer.rebuildWithFilter(filter))
                .toList();
        return new QueuedMultiplePlacer<>(factory, configPolicy, filteredQueuedPlacerList);
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        var iterator = new MultipleQueuedPlacingIterator(queuedPlacerList);
        phaseLifecycleSupport.addEventListener(iterator);
        return iterator;
    }

    private class MultipleQueuedPlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>>
            implements PhaseLifecycleListener<Solution_> {

        private final List<EntityPlacer<Solution_>> queuedPlacerList;
        private List<Iterator<Placement<Solution_>>> placementIteratorList;
        private List<Iterator<Move<Solution_>>> moveIteratorList;
        private Move<Solution_>[] previousMove;
        private Move<Solution_> cachedMove = null;

        private MultipleQueuedPlacingIterator(List<EntityPlacer<Solution_>> queuedPlacerList) {
            this.queuedPlacerList = new ArrayList<>(queuedPlacerList);
            reset();
        }

        /**
         * The method uses a strategy similar to {@link CartesianProductMoveSelector},
         * but it uses placer iterators instead.
         */
        private Move<Solution_> nextMove() {
            if (cachedMove == null) {
                var childSize = moveIteratorList.size();
                int index;
                Move<Solution_>[] move = new Move[childSize];
                if (previousMove == null) {
                    index = -1;
                } else {
                    index = consumeNextMove(move, previousMove);
                    if (index == -1) {
                        // No more moves
                        return null;
                    }
                }
                var recreated = recreateNextIterators(index, move);
                if (!recreated) {
                    // We stop if one of the placement iterators has no next placement
                    return null;
                }
                previousMove = move;
                cachedMove = CompositeMove.buildMove(move);
            }
            return cachedMove;
        }

        /**
         * Go through the registered iterators and check for any available moves.
         *
         * @param move the move array to be loaded
         * @param previousMove the previous moves
         * @return the last index of the iterator that still has available moves; otherwise, the function returns -1
         *         when all iterators have no more moves available.
         */
        private int consumeNextMove(Move<?>[] move, Move<Solution_>[] previousMove) {
            var index = move.length - 1;
            // Look for the first iterator that still has available moves to generate
            while (index >= 0) {
                var moveIterator = moveIteratorList.get(index);
                if (moveIterator.hasNext()) {
                    break;
                }
                index--;
            }
            if (index < 0) {
                return -1;
            }
            // Copy the previous move until the next one generated
            System.arraycopy(previousMove, 0, move, 0, index);
            // Generate and set the new move
            move[index] = moveIteratorList.get(index).next();
            return index;
        }

        /**
         * Recreate all move iterators starting from #lastValidIteratorIndex.
         *
         * @param lastValidIteratorIndex the index of the last iterator that generated a valid move
         * @param move the move array to be loaded
         */
        private boolean recreateNextIterators(int lastValidIteratorIndex, Move<?>[] move) {
            for (int i = lastValidIteratorIndex + 1; i < move.length; i++) {
                var placementIterator = queuedPlacerList.get(i).iterator();
                placementIteratorList.set(i, placementIterator);
                Move<Solution_> next;
                if (!placementIterator.hasNext()) {
                    return false;
                } else {
                    var moveIterator = placementIterator.next().iterator();
                    moveIteratorList.set(i, moveIterator);
                    next = moveIterator.next();
                }
                move[i] = next;
            }
            return true;
        }

        private void clearCache() {
            this.cachedMove = null;
        }

        private void reset() {
            placementIteratorList = new ArrayList<>(queuedPlacerList.size());
            moveIteratorList = new ArrayList<>(queuedPlacerList.size());
            for (EntityPlacer<Solution_> placements : queuedPlacerList) {
                moveIteratorList.add(null);
                placementIteratorList.add(null);
            }
            previousMove = null;
        }

        @Override
        protected Placement<Solution_> createUpcomingSelection() {
            var nextMove = nextMove();
            if (nextMove == null) {
                return noUpcomingSelection();
            }
            return new Placement<>(new PlacementToMoveAdapterIterator(this));
        }

        @Override
        public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
            // Ignore
        }

        @Override
        public void stepStarted(AbstractStepScope<Solution_> stepScope) {
            // Ignore
        }

        @Override
        public void stepEnded(AbstractStepScope<Solution_> stepScope) {
            reset();
        }

        @Override
        public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
            // Ignore
        }

        @Override
        public void solvingStarted(SolverScope<Solution_> solverScope) {
            // Ignore
        }

        @Override
        public void solvingEnded(SolverScope<Solution_> solverScope) {
            // Ignore
        }
    }

    private class PlacementToMoveAdapterIterator implements Iterator<Move<Solution_>> {
        private final MultipleQueuedPlacingIterator iterator;

        private PlacementToMoveAdapterIterator(MultipleQueuedPlacingIterator iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.nextMove() != null;
        }

        @Override
        public Move<Solution_> next() {
            var move = iterator.nextMove();
            iterator.clearCache();
            return move;
        }
    }
}
