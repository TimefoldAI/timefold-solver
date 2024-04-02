package ai.timefold.solver.core.impl.heuristic.selector.entity.decorator;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.Objects;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionListIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

public final class FilteringEntitySelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements EntitySelector<Solution_> {

    public static <Solution_> FilteringEntitySelector<Solution_> of(EntitySelector<Solution_> childEntitySelector,
            SelectionFilter<Solution_, Object> filter) {
        if (childEntitySelector instanceof FilteringEntitySelector<Solution_> filteringEntitySelector) {
            return new FilteringEntitySelector<>(filteringEntitySelector.childEntitySelector,
                    SelectionFilter.compose(filteringEntitySelector.selectionFilter, filter));
        }
        return new FilteringEntitySelector<>(childEntitySelector, filter);
    }

    private final EntitySelector<Solution_> childEntitySelector;
    private final SelectionFilter<Solution_, Object> selectionFilter;
    private final boolean bailOutEnabled;

    private ScoreDirector<Solution_> scoreDirector = null;

    private FilteringEntitySelector(EntitySelector<Solution_> childEntitySelector, SelectionFilter<Solution_, Object> filter) {
        this.childEntitySelector = Objects.requireNonNull(childEntitySelector);
        this.selectionFilter = Objects.requireNonNull(filter);
        bailOutEnabled = childEntitySelector.isNeverEnding();
        phaseLifecycleSupport.addEventListener(childEntitySelector);
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
    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return childEntitySelector.getEntityDescriptor();
    }

    @Override
    public boolean isCountable() {
        return childEntitySelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return childEntitySelector.isNeverEnding();
    }

    @Override
    public long getSize() {
        return childEntitySelector.getSize();
    }

    @Override
    public Iterator<Object> iterator() {
        return new JustInTimeFilteringEntityIterator(childEntitySelector.iterator(), determineBailOutSize());
    }

    protected class JustInTimeFilteringEntityIterator extends UpcomingSelectionIterator<Object> {

        private final Iterator<Object> childEntityIterator;
        private final long bailOutSize;

        public JustInTimeFilteringEntityIterator(Iterator<Object> childEntityIterator, long bailOutSize) {
            this.childEntityIterator = childEntityIterator;
            this.bailOutSize = bailOutSize;
        }

        @Override
        protected Object createUpcomingSelection() {
            Object next;
            long attemptsBeforeBailOut = bailOutSize;
            do {
                if (!childEntityIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                if (bailOutEnabled) {
                    // if childEntityIterator is neverEnding and nothing is accepted, bail out of the infinite loop
                    if (attemptsBeforeBailOut <= 0L) {
                        logger.trace("Bailing out of neverEnding selector ({}) to avoid infinite loop.",
                                FilteringEntitySelector.this);
                        return noUpcomingSelection();
                    }
                    attemptsBeforeBailOut--;
                }
                next = childEntityIterator.next();
            } while (!selectionFilter.accept(scoreDirector, next));
            return next;
        }

    }

    protected class JustInTimeFilteringEntityListIterator extends UpcomingSelectionListIterator<Object> {
        private final ListIterator<Object> childEntityListIterator;

        public JustInTimeFilteringEntityListIterator(ListIterator<Object> childEntityListIterator) {
            this.childEntityListIterator = childEntityListIterator;
        }

        @Override
        protected Object createUpcomingSelection() {
            Object next;
            do {
                if (!childEntityListIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                next = childEntityListIterator.next();
            } while (!selectionFilter.accept(scoreDirector, next));
            return next;
        }

        @Override
        protected Object createPreviousSelection() {
            Object previous;
            do {
                if (!childEntityListIterator.hasPrevious()) {
                    return noPreviousSelection();
                }
                previous = childEntityListIterator.previous();
            } while (!selectionFilter.accept(scoreDirector, previous));
            return previous;
        }
    }

    @Override
    public ListIterator<Object> listIterator() {
        return new JustInTimeFilteringEntityListIterator(childEntitySelector.listIterator());
    }

    @Override
    public ListIterator<Object> listIterator(int index) {
        JustInTimeFilteringEntityListIterator listIterator =
                new JustInTimeFilteringEntityListIterator(childEntitySelector.listIterator());
        for (int i = 0; i < index; i++) {
            listIterator.next();
        }
        return listIterator;
    }

    @Override
    public Iterator<Object> endingIterator() {
        return new JustInTimeFilteringEntityIterator(childEntitySelector.endingIterator(), determineBailOutSize());
    }

    private long determineBailOutSize() {
        if (!bailOutEnabled) {
            return -1L;
        }
        return childEntitySelector.getSize() * 10L;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FilteringEntitySelector<?> that
                && Objects.equals(childEntitySelector, that.childEntitySelector)
                && Objects.equals(selectionFilter, that.selectionFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childEntitySelector, selectionFilter);
    }

    @Override
    public String toString() {
        return "Filtering(" + childEntitySelector + ")";
    }

}
