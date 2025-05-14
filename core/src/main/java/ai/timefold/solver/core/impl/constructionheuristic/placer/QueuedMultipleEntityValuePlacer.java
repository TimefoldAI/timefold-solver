package ai.timefold.solver.core.impl.constructionheuristic.placer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.move.generic.CompositeMove;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

public class QueuedMultipleEntityValuePlacer<Solution_> extends AbstractEntityPlacer<Solution_>
        implements EntityPlacer<Solution_> {

    private final List<EntityPlacer<Solution_>> queuedPlacerList;
    private final boolean sequentialSelection;

    public QueuedMultipleEntityValuePlacer(EntityPlacerFactory<Solution_> factory,
            HeuristicConfigPolicy<Solution_> configPolicy, List<EntityPlacer<Solution_>> queuedPlacerList,
            boolean sequentialSelection) {
        super(factory, configPolicy);
        this.queuedPlacerList = queuedPlacerList;
        this.sequentialSelection = sequentialSelection;
        this.queuedPlacerList.forEach(queuedPlacer -> phaseLifecycleSupport.addEventListener(queuedPlacer));

    }

    @Override
    public EntityPlacer<Solution_> rebuildWithFilter(SelectionFilter<Solution_, Object> filter) {
        var filteredQueuedPlacerList = queuedPlacerList.stream()
                .map(placer -> placer.rebuildWithFilter(filter))
                .toList();
        return new QueuedMultipleEntityValuePlacer<>(factory, configPolicy, filteredQueuedPlacerList, sequentialSelection);
    }

    @Override
    public Iterator<Placement<Solution_>> iterator() {
        if (sequentialSelection) {
            return new SequentialQueuedEntityValuePlacingIterator(queuedPlacerList);
        } else {
            var iterator = new CartesianProductQueuedEntityValuePlacingIterator(queuedPlacerList);
            phaseLifecycleSupport.addEventListener(iterator);
            return iterator;
        }
    }

    private class SequentialQueuedEntityValuePlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>> {

        private final List<EntityPlacer<Solution_>> queuedPlacerList;
        private EntityPlacer<Solution_> currentPlacer;
        private int index = 0;

        private SequentialQueuedEntityValuePlacingIterator(List<EntityPlacer<Solution_>> queuedPlacerList) {
            this.queuedPlacerList = queuedPlacerList;
        }

        @Override
        protected Placement<Solution_> createUpcomingSelection() {
            if (pickNextPlacer()) {
                return currentPlacer.iterator().next();
            }
            return noUpcomingSelection();
        }

        private boolean pickNextPlacer() {
            if (currentPlacer != null && currentPlacer.iterator().hasNext()) {
                return true;
            }
            while (index < queuedPlacerList.size()) {
                currentPlacer = queuedPlacerList.get(index);
                if (!currentPlacer.iterator().hasNext()) {
                    index++;
                    continue;
                }
                return true;
            }
            return false;
        }
    }

    private class CartesianProductQueuedEntityValuePlacingIterator extends UpcomingSelectionIterator<Placement<Solution_>>
            implements PhaseLifecycleListener<Solution_> {

        private final List<EntityPlacer<Solution_>> queuedPlacerList;
        private List<Iterator<Placement<Solution_>>> placementIteratorList;
        private List<Iterator<Move<Solution_>>> moveIteratorList;
        private Move<Solution_>[] subSelections;
        private Move<Solution_> cachedMove = null;

        private CartesianProductQueuedEntityValuePlacingIterator(List<EntityPlacer<Solution_>> queuedPlacerList) {
            // The logic used to generate the Cartesian product assumes the first placer is QueuedValuePlacer (list variable)
            this.queuedPlacerList = new ArrayList<>(queuedPlacerList);
            this.queuedPlacerList.sort((p1, p2) -> p1 instanceof QueuedValuePlacer<Solution_> ? -1 : 1);
            reset();
        }

        private Move<Solution_> nextMove() {
            if (cachedMove == null) {
                var childSize = moveIteratorList.size();
                int startingIndex;
                var moveList = new Move[childSize];
                if (subSelections == null) {
                    startingIndex = -1;
                } else {
                    startingIndex = childSize - 1;
                    while (startingIndex >= 0) {
                        var moveIterator = moveIteratorList.get(startingIndex);
                        if (moveIterator.hasNext()) {
                            break;
                        } else if (startingIndex == 0) {
                            // When the QueuedValuePlacer move iterator is exhausted,
                            // we need to double-check that there are no more values to place;
                            // otherwise, we might miss testing some available values.
                            var placementIterator = placementIteratorList.get(startingIndex);
                            if (placementIterator.hasNext()) {
                                moveIteratorList.set(startingIndex, placementIterator.next().iterator());
                                continue;
                            }
                        }
                        startingIndex--;
                    }
                    if (startingIndex < 0) {
                        return null;
                    }
                    System.arraycopy(subSelections, 0, moveList, 0, startingIndex);
                    moveList[startingIndex] = moveIteratorList.get(startingIndex).next();
                }
                for (int i = startingIndex + 1; i < childSize; i++) {
                    var placementIterator = queuedPlacerList.get(i).iterator();
                    placementIteratorList.set(i, placementIterator);
                    Move<Solution_> next;
                    if (!placementIterator.hasNext()) { // in case a moveIterator is empty
                        return null;
                    } else {
                        var moveIterator = placementIterator.next().iterator();
                        moveIteratorList.set(i, moveIterator);
                        next = moveIterator.next();
                    }
                    moveList[i] = next;
                }
                subSelections = moveList;
                cachedMove = CompositeMove.buildMove(moveList);
            }
            return cachedMove;
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
            subSelections = null;
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
        private final CartesianProductQueuedEntityValuePlacingIterator iterator;

        private PlacementToMoveAdapterIterator(CartesianProductQueuedEntityValuePlacingIterator iterator) {
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
