package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.UpcomingSelectionIterator;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;

public class FilteringValueSelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements ValueSelector<Solution_> {

    public static <Solution_> ValueSelector<Solution_> of(ValueSelector<Solution_> valueSelector,
            SelectionFilter<Solution_, Object> filter) {
        if (valueSelector instanceof EntityIndependentFilteringValueSelector<Solution_> filteringValueSelector) {
            return new EntityIndependentFilteringValueSelector<>(
                    (EntityIndependentValueSelector<Solution_>) filteringValueSelector.childValueSelector,
                    SelectionFilter.compose(filteringValueSelector.selectionFilter, filter));
        } else if (valueSelector instanceof FilteringValueSelector<Solution_> filteringValueSelector) {
            return new FilteringValueSelector<>(filteringValueSelector.childValueSelector,
                    SelectionFilter.compose(filteringValueSelector.selectionFilter, filter));
        } else if (valueSelector instanceof EntityIndependentValueSelector<Solution_> entityIndependentValueSelector) {
            return new EntityIndependentFilteringValueSelector<>(entityIndependentValueSelector, filter);
        } else {
            return new FilteringValueSelector<>(valueSelector, filter);
        }
    }

    public static <Solution_> ValueSelector<Solution_> ofAssigned(ValueSelector<Solution_> valueSelector,
            Supplier<ListVariableStateSupply<Solution_>> listVariableStateSupplier) {
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) valueSelector.getVariableDescriptor();
        if (!listVariableDescriptor.allowsUnassignedValues()) {
            return valueSelector;
        }
        // We need to filter out unassigned vars.
        return FilteringValueSelector.of(valueSelector, (scoreDirector, selection) -> {
            var listVariableStateSupply = listVariableStateSupplier.get();
            if (listVariableStateSupply.getUnassignedCount() == 0) {
                return true;
            }
            return listVariableStateSupply.isAssigned(selection);
        });
    }

    public static <Solution_> EntityIndependentValueSelector<Solution_> ofAssigned(
            EntityIndependentValueSelector<Solution_> entityIndependentValueSelector,
            Supplier<ListVariableStateSupply<Solution_>> listVariableStateSupplier) {
        return (EntityIndependentValueSelector<Solution_>) ofAssigned((ValueSelector<Solution_>) entityIndependentValueSelector,
                listVariableStateSupplier);
    }

    protected final ValueSelector<Solution_> childValueSelector;
    final SelectionFilter<Solution_, Object> selectionFilter;
    protected final boolean bailOutEnabled;

    private ScoreDirector<Solution_> scoreDirector = null;

    protected FilteringValueSelector(ValueSelector<Solution_> childValueSelector, SelectionFilter<Solution_, Object> filter) {
        this.childValueSelector = Objects.requireNonNull(childValueSelector);
        this.selectionFilter = Objects.requireNonNull(filter);
        bailOutEnabled = childValueSelector.isNeverEnding();
        phaseLifecycleSupport.addEventListener(childValueSelector);
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
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return childValueSelector.getVariableDescriptor();
    }

    @Override
    public boolean isCountable() {
        return childValueSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return childValueSelector.isNeverEnding();
    }

    @Override
    public long getSize(Object entity) {
        return childValueSelector.getSize(entity);
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        return new JustInTimeFilteringValueIterator(childValueSelector.iterator(entity),
                determineBailOutSize(entity));
    }

    protected class JustInTimeFilteringValueIterator extends UpcomingSelectionIterator<Object> {

        private final Iterator<Object> childValueIterator;
        private final long bailOutSize;

        public JustInTimeFilteringValueIterator(Iterator<Object> childValueIterator, long bailOutSize) {
            this.childValueIterator = childValueIterator;
            this.bailOutSize = bailOutSize;
        }

        @Override
        protected Object createUpcomingSelection() {
            Object next;
            long attemptsBeforeBailOut = bailOutSize;
            do {
                if (!childValueIterator.hasNext()) {
                    return noUpcomingSelection();
                }
                if (bailOutEnabled) {
                    // if childValueIterator is neverEnding and nothing is accepted, bail out of the infinite loop
                    if (attemptsBeforeBailOut <= 0L) {
                        logger.trace("Bailing out of neverEnding selector ({}) to avoid infinite loop.",
                                FilteringValueSelector.this);
                        return noUpcomingSelection();
                    }
                    attemptsBeforeBailOut--;
                }
                next = childValueIterator.next();
            } while (!selectionFilter.accept(scoreDirector, next));
            return next;
        }

    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        return new JustInTimeFilteringValueIterator(childValueSelector.endingIterator(entity),
                determineBailOutSize(entity));
    }

    protected long determineBailOutSize(Object entity) {
        if (!bailOutEnabled) {
            return -1L;
        }
        return childValueSelector.getSize(entity) * 10L;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FilteringValueSelector<?> that
                && Objects.equals(childValueSelector, that.childValueSelector)
                && Objects.equals(selectionFilter, that.selectionFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(childValueSelector, selectionFilter);
    }

    @Override
    public String toString() {
        return "Filtering(" + childValueSelector + ")";
    }

}
