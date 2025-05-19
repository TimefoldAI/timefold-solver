package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;
import java.util.Arrays;
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
        private Iterator<Move<Solution_>>[] moveIterators;
        private Iterator<Placement<Solution_>>[] placementIterators;
        private Move<Solution_>[] previousMove;
        private Move<Solution_> cachedMove = null;

        private MultipleQueuedPlacingIterator(List<EntityPlacer<Solution_>> queuedPlacerList) {
            // We expect only the QueuedValuePlacer and a QueuedEntityPlacer
            var assertSize = queuedPlacerList.size() == 2;
            var assertQueuedValuePlacer =
                    queuedPlacerList.stream().anyMatch(QueuedValuePlacer.class::isInstance);
            var assertQueuedEntityPlacer =
                    queuedPlacerList.stream().anyMatch(QueuedEntityPlacer.class::isInstance);
            if (!assertSize || !assertQueuedValuePlacer || !assertQueuedEntityPlacer) {
                throw new IllegalArgumentException(
                        "Impossible state: the queued placer list must consist exclusively of a QueuedValuePlacer and a QueuedEntityPlacer.");
            }
            this.queuedPlacerList = new ArrayList<>();
            // We make sure that the QueuedEntityPlacer is added first
            this.queuedPlacerList.addAll(queuedPlacerList.stream().filter(QueuedValuePlacer.class::isInstance).toList());
            this.queuedPlacerList.addAll(queuedPlacerList.stream().filter(QueuedEntityPlacer.class::isInstance).toList());
            reset();
        }

        /**
         * The method uses a strategy similar to {@link CartesianProductMoveSelector},
         * but it uses placer iterators instead.
         */
        private Move<Solution_> nextMove() {
            if (cachedMove != null) {
                return cachedMove;
            }
            var childSize = moveIterators.length;
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
            var updatedMove = updateNextIterators(index, move);
            if (updatedMove == null) {
                // We stop if one of the placement iterators has no next placement
                return null;
            }
            previousMove = updatedMove;
            cachedMove = CompositeMove.buildMove(updatedMove);
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
                var moveIterator = moveIterators[index];
                if (moveIterator.hasNext()) {
                    break;
                }
                // Check if there are more placements available in the QueuedEntityPlacer
                if (index == 1) {
                    var placementIterator = placementIterators[index];
                    if (placementIterator.hasNext()) {
                        moveIterators[index] = placementIterator.next().iterator();
                        continue;
                    } else {
                        // Reset the iterator in case the previous placerIterator still has more placements
                        placementIterators[index] = queuedPlacerList.get(index).iterator();
                    }
                }
                index--;
            }
            if (index < 0) {
                return -1;
            }
            // Copy the previous move until the next one generated
            System.arraycopy(previousMove, 0, move, 0, index);
            // Generate and set the new move
            move[index] = moveIterators[index].next();
            return index;
        }

        /**
         * Update the move list and recreate all move iterators starting from #lastValidIteratorIndex.
         *
         * @param lastValidIteratorIndex the index of the last iterator that generated a valid move
         * @param move the move array to be loaded
         */
        private Move<Solution_>[] updateNextIterators(int lastValidIteratorIndex, Move<?>[] move) {
            var childSize = moveIterators.length;
            var updatedMove = new Move[childSize];
            System.arraycopy(move, 0, updatedMove, 0, childSize);
            for (int i = lastValidIteratorIndex + 1; i < childSize; i++) {
                var placementIterator = placementIterators[i];
                Move<Solution_> next;
                if (!placementIterator.hasNext()) {
                    return null;
                } else {
                    var moveIterator = placementIterator.next().iterator();
                    moveIterators[i] = moveIterator;
                    next = moveIterator.next();
                }
                updatedMove[i] = next;
            }
            return updatedMove;
        }

        private void clearCache() {
            this.cachedMove = null;
        }

        @SuppressWarnings("unchecked")
        private void reset() {
            if (moveIterators == null) {
                moveIterators = new Iterator[queuedPlacerList.size()];
                Arrays.fill(moveIterators, null);
                placementIterators = new Iterator[queuedPlacerList.size()];
                for (var i = 0; i < queuedPlacerList.size(); i++) {
                    var placement = queuedPlacerList.get(i);
                    placementIterators[i] = placement.iterator();
                }
            } else {
                Arrays.fill(moveIterators, null);
                // We need to reset of the QueuedEntityPlacer or there will be no more moves for the basic variables
                placementIterators[1] = queuedPlacerList.get(1).iterator();
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
