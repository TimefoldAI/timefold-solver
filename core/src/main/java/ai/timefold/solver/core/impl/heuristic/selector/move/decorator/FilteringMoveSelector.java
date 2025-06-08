package ai.timefold.solver.core.impl.heuristic.selector.move.decorator;

import java.util.Iterator;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

public final class FilteringMoveSelector<Solution_> extends AbstractMoveSelector<Solution_> {

    private static final long BAIL_OUT_MULTIPLIER = 10L;

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
    private AbstractPhaseScope<Solution_> phaseScope;

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
        this.scoreDirector = phaseScope.getScoreDirector();
        this.phaseScope = phaseScope;
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.scoreDirector = null;
        this.phaseScope = null;
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
        return new JustInTimeFilteringMoveIterator(childMoveSelector.iterator(), determineBailOutSize(), phaseScope);
    }

    private long determineBailOutSize() {
        if (!bailOutEnabled) {
            return -1L;
        }
        try {
            return childMoveSelector.getSize() * BAIL_OUT_MULTIPLIER;
        } catch (Exception ex) {
            // Some move selectors throw an exception when getSize() is called.
            // In this case, we choose to disregard it and pick a large-enough bail-out size anyway.
            // The ${bailOutSize+1}th move could in theory show up where previous ${bailOutSize} moves did not,
            // but we consider this to be an acceptable risk,
            // outweighed by the benefit of the solver never running into an endless loop.
            // The exception itself is swallowed, as it doesn't bring any useful information.
            long bailOutSize = Short.MAX_VALUE * BAIL_OUT_MULTIPLIER;
            logger.trace(
                    "        Never-ending move selector ({}) failed to provide size, choosing a bail-out size of ({}) attempts.",
                    childMoveSelector, bailOutSize);
            return bailOutSize;
        }
    }

    private class JustInTimeFilteringMoveIterator extends UpcomingSelectionIterator<Move<Solution_>> {

        private final long TERMINATION_BAIL_OUT_SIZE = 1000L;
        private final Iterator<Move<Solution_>> childMoveIterator;
        private final long bailOutSize;
        private final AbstractPhaseScope<Solution_> phaseScope;
        private final PhaseTermination<Solution_> termination;

        public JustInTimeFilteringMoveIterator(Iterator<Move<Solution_>> childMoveIterator, long bailOutSize,
                AbstractPhaseScope<Solution_> phaseScope) {
            this.childMoveIterator = childMoveIterator;
            this.bailOutSize = bailOutSize;
            this.phaseScope = phaseScope;
            this.termination = phaseScope != null ? phaseScope.getTermination() : null;
        }

        @Override
        protected Move<Solution_> createUpcomingSelection() {
            Move<Solution_> next;
            long attemptsBeforeBailOut = bailOutSize;
            // To reduce the impact of checking for termination on each move,
            // we only check for termination after filtering out 1000 moves.
            long attemptsBeforeCheckTermination = TERMINATION_BAIL_OUT_SIZE;
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
                    } else if (termination != null && attemptsBeforeCheckTermination <= 0L) {
                        // Reset the counter
                        attemptsBeforeCheckTermination = TERMINATION_BAIL_OUT_SIZE;
                        if (termination.isPhaseTerminated(phaseScope)) {
                            logger.trace(
                                    "Bailing out of neverEnding selector ({}) because the termination setting has been triggered.",
                                    FilteringMoveSelector.this);
                            return noUpcomingSelection();
                        }
                    }
                    attemptsBeforeBailOut--;
                    attemptsBeforeCheckTermination--;
                }
                next = childMoveIterator.next();
            } while (!accept(scoreDirector, next));
            return next;
        }

    }

    private boolean accept(ScoreDirector<Solution_> scoreDirector, Move<Solution_> move) {
        if (filter != null && !filter.accept(scoreDirector, move)) {
            logger.trace("        Move ({}) filtered out by a selection filter ({}).", move, filter);
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Filtering(" + childMoveSelector + ")";
    }

}
