package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

public final class FilteringMoveSelector<Solution_> extends AbstractMoveSelector<Solution_> {

    public static <Solution_> FilteringMoveSelector<Solution_> of(MoveSelector<Solution_> moveSelector,
            SelectionFilter<Solution_, Move<Solution_>> filter) {
        if (moveSelector instanceof FilteringMoveSelector<Solution_> filteringMoveSelector) {
            return new FilteringMoveSelector<>(filteringMoveSelector.childMoveSelector,
                    SelectionFilter.compose(filteringMoveSelector.filter, filter));
        }
        return new FilteringMoveSelector<>(moveSelector, filter);
    }

    private final MoveSelector<Solution_> childMoveSelector;
    private final SelectionFilter<Solution_, Move<Solution_>> filter;
    private final boolean bailOutEnabled;

    private ScoreDirector<Solution_> scoreDirector = null;

    private FilteringMoveSelector(MoveSelector<Solution_> childMoveSelector,
            SelectionFilter<Solution_, Move<Solution_>> filter) {
        this.childMoveSelector = childMoveSelector;
        this.filter = filter;
        bailOutEnabled = childMoveSelector.isNeverEnding();
        phaseLifecycleSupport.addEventListener(childMoveSelector);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        scoreDirector = phaseScope.getScoreDirector();
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        scoreDirector = null;
    }

    @Override
    public boolean isCountable() {
        return childMoveSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return childMoveSelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        return childMoveSelector.getSize();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new JustInTimeFilteringMoveIterator(childMoveSelector.iterator(), determineBailOutSize());
    }

    private class JustInTimeFilteringMoveIterator extends UpcomingSelectionIterator<Move<Solution_>> {

        private final Iterator<Move<Solution_>> childMoveIterator;
        private final long bailOutSize;

        public JustInTimeFilteringMoveIterator(Iterator<Move<Solution_>> childMoveIterator, long bailOutSize) {
            this.childMoveIterator = childMoveIterator;
            this.bailOutSize = bailOutSize;
        }

        @Override
        protected Move<Solution_> createUpcomingSelection() {
            Move<Solution_> next;
            long attemptsBeforeBailOut = bailOutSize;
            do {
                if (!childMoveIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                if (bailOutEnabled) {
                    // if childMoveIterator is neverEnding and nothing is accepted, bail out of the infinite loop
                    if (attemptsBeforeBailOut <= 0L) {
                        logger.trace("Bailing out of neverEnding selector ({}) after ({}) attempts to avoid infinite loop.",
                                FilteringMoveSelector.this, bailOutSize);
                        return noUpcomingSelection();
                    }
                    attemptsBeforeBailOut--;
                }
                next = childMoveIterator.next();
            } while (!accept(scoreDirector, next));
            return next;
        }

    }

    private long determineBailOutSize() {
        if (!bailOutEnabled) {
            return -1L;
        }
        try {
            return childMoveSelector.getSize() * 10L;
        } catch (Exception ex) {
            /*
             * Some move selectors throw an exception when getSize() is called.
             * In this case, we choose to disregard it and pick a large-enough bail-out size anyway.
             * The ${bailOutSize+1}th move could in theory show up where previous ${bailOutSize} moves did not,
             * but we consider this to be an acceptable risk,
             * outweighed by the benefit of the solver never running into an endless loop.
             */
            long bailOutSize = Short.MAX_VALUE * 10L;
            logger.trace(
                    "        Never-ending move selector ({}) failed to provide size, choosing a bail-out size of ({}) attempts.",
                    childMoveSelector, bailOutSize, ex);
            return bailOutSize;
        }
    }

    private boolean accept(ScoreDirector<Solution_> scoreDirector, Move<Solution_> move) {
        if (filter != null) {
            if (!filter.accept(scoreDirector, move)) {
                logger.trace("        Move ({}) filtered out by a selection filter ({}).", move, filter);
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "Filtering(" + childMoveSelector + ")";
    }

}
