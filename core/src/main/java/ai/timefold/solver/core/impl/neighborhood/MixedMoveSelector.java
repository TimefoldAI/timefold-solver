package ai.timefold.solver.core.impl.neighborhood;

import java.util.Iterator;
import java.util.List;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.CompositeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.UnionMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.AbstractCachingMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.FilteringMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.SelectedCountLimitMoveSelector;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 * Mixes a legacy {@link MoveSelector} with a {@link NeighborhoodsMoveSelector},
 * drawing from each side in proportion to how many movers it holds:
 * the number of leaf move selectors on one side, the number of neighborhoods on the other.
 *
 * @see NeighborhoodsMoveSelector
 */
public final class MixedMoveSelector<Solution_> extends CompositeMoveSelector<Solution_> {

    private final MoveSelector<Solution_> moveSelector;
    private final NeighborhoodsMoveSelector<Solution_> neighborhoodsMoveSelector;
    private final int moveSelectorWeight;
    private final int neighborhoodWeight;

    public MixedMoveSelector(MoveSelector<Solution_> moveSelector,
            NeighborhoodsMoveSelector<Solution_> neighborhoodsMoveSelector) {
        super(List.of(moveSelector, neighborhoodsMoveSelector), true);
        this.moveSelector = moveSelector;
        this.neighborhoodsMoveSelector = neighborhoodsMoveSelector;
        this.moveSelectorWeight = countMoveSelectors(moveSelector);
        this.neighborhoodWeight = neighborhoodsMoveSelector.getNeighborhoodCount();
    }

    /**
     * Counts the leaf move selectors reachable from the given selector,
     * recursing through nested unions and through the decorators
     * (filtering, caching/sorting/shuffling, and selected-count-limit) that may wrap them.
     * A {@code ProbabilityMoveSelector} is deliberately left as an opaque leaf:
     * it weights a single selector's own moves, which is unrelated to weighting one selector
     * against another.
     */
    static int countMoveSelectors(MoveSelector<?> moveSelector) {
        if (moveSelector instanceof UnionMoveSelector<?> unionMoveSelector) {
            if (unionMoveSelector.getSelectorProbabilityWeightFactory() != null) {
                throw new UnsupportedOperationException(
                        "Probability-weighted move selectors are not supported together with the Neighborhoods API.");
            }
            var count = 0;
            for (var childMoveSelector : unionMoveSelector.getChildMoveSelectorList()) {
                count += countMoveSelectors(childMoveSelector);
            }
            return count;
        } else if (moveSelector instanceof FilteringMoveSelector<?> filteringMoveSelector) {
            return countMoveSelectors(filteringMoveSelector.getChildMoveSelector());
        } else if (moveSelector instanceof AbstractCachingMoveSelector<?> cachingMoveSelector) {
            return countMoveSelectors(cachingMoveSelector.getChildMoveSelector());
        } else if (moveSelector instanceof SelectedCountLimitMoveSelector<?> limitMoveSelector) {
            return countMoveSelectors(limitMoveSelector.getChildMoveSelector());
        }
        return 1;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public long getSize() {
        throw new UnsupportedOperationException("Neighborhood size is not supported by the Neighborhoods API.");
    }

    @Override
    public boolean isNeverEnding() {
        return moveSelector.isNeverEnding() || neighborhoodsMoveSelector.isNeverEnding();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new MixedMoveIterator<>(moveSelector.iterator(), moveSelectorWeight,
                neighborhoodsMoveSelector.iterator(), neighborhoodWeight, workingRandom);
    }

    @Override
    public String toString() {
        return "Mixed(%s, %s)"
                .formatted(moveSelector, neighborhoodsMoveSelector);
    }

    private static final class MixedMoveIterator<Solution_> implements Iterator<Move<Solution_>> {

        private final Iterator<Move<Solution_>> moveSelectorIterator;
        private final Iterator<Move<Solution_>> neighborhoodIterator;
        private final int moveSelectorWeight;
        private final int weightTotal;
        private final RandomGenerator workingRandom;

        MixedMoveIterator(Iterator<Move<Solution_>> moveSelectorIterator, int moveSelectorWeight,
                Iterator<Move<Solution_>> neighborhoodIterator, int neighborhoodWeight, RandomGenerator workingRandom) {
            this.moveSelectorIterator = moveSelectorIterator;
            this.neighborhoodIterator = neighborhoodIterator;
            this.moveSelectorWeight = moveSelectorWeight;
            this.weightTotal = moveSelectorWeight + neighborhoodWeight;
            this.workingRandom = workingRandom;
        }

        @Override
        public boolean hasNext() {
            return moveSelectorIterator.hasNext() || neighborhoodIterator.hasNext();
        }

        @Override
        public Move<Solution_> next() {
            if (!moveSelectorIterator.hasNext()) { // Collapse to the surviving side.
                return neighborhoodIterator.next();
            } else if (!neighborhoodIterator.hasNext()) {
                return moveSelectorIterator.next();
            }
            return workingRandom.nextInt(weightTotal) < moveSelectorWeight
                    ? moveSelectorIterator.next()
                    : neighborhoodIterator.next();
        }

    }

}
