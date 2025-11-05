package ai.timefold.solver.core.impl.heuristic.selector.value;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractDemandEnabledSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * This is the common {@link ValueSelector} implementation.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class FromEntityPropertyValueSelector<Solution_>
        extends AbstractDemandEnabledSelector<Solution_>
        implements ValueSelector<Solution_> {

    private final ValueRangeDescriptor<Solution_> valueRangeDescriptor;
    private final SelectionSorter<Solution_, Object> selectionSorter;
    private final boolean randomSelection;

    private CountableValueRange<Object> countableValueRange;
    private InnerScoreDirector<Solution_, ?> scoreDirector;

    public FromEntityPropertyValueSelector(ValueRangeDescriptor<Solution_> valueRangeDescriptor,
            SelectionSorter<Solution_, Object> selectionSorter, boolean randomSelection) {
        this.valueRangeDescriptor = valueRangeDescriptor;
        this.selectionSorter = selectionSorter;
        this.randomSelection = randomSelection;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.scoreDirector = solverScope.getScoreDirector();
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        this.scoreDirector = null;
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        this.countableValueRange = scoreDirector.getValueRangeManager().getFromSolution(valueRangeDescriptor, selectionSorter);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.countableValueRange = null;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public SelectionSorter<Solution_, Object> getSelectionSorter() {
        return selectionSorter;
    }

    @Override
    public GenuineVariableDescriptor<Solution_> getVariableDescriptor() {
        return valueRangeDescriptor.getVariableDescriptor();
    }

    @Override
    public boolean isCountable() {
        return valueRangeDescriptor.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || !isCountable();
    }

    @Override
    public long getSize(Object entity) {
        if (entity == null) {
            // When the entity is null, the size of the complete list of values is returned
            // This logic aligns with the requirements for Nearby in the enterprise repository
            return Objects.requireNonNull(countableValueRange).getSize();
        } else {
            return scoreDirector.getValueRangeManager().countOnEntity(valueRangeDescriptor, entity);
        }
    }

    @Override
    public Iterator<Object> iterator(Object entity) {
        var valueRange = scoreDirector.getValueRangeManager().getFromEntity(valueRangeDescriptor, entity, selectionSorter);
        if (!randomSelection) {
            return valueRange.createOriginalIterator();
        } else {
            return valueRange.createRandomIterator(workingRandom);
        }
    }

    @Override
    public Iterator<Object> endingIterator(Object entity) {
        if (entity == null) {
            // When the entity is null, the complete list of values is returned
            // This logic aligns with the requirements for Nearby in the enterprise repository
            return countableValueRange.createOriginalIterator();
        } else {
            var valueRange = scoreDirector.getValueRangeManager().getFromEntity(valueRangeDescriptor, entity, selectionSorter);
            return valueRange.createOriginalIterator();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FromEntityPropertyValueSelector<?> that))
            return false;
        return Objects.equals(valueRangeDescriptor, that.valueRangeDescriptor)
                && Objects.equals(selectionSorter, that.selectionSorter)
                && randomSelection == that.randomSelection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueRangeDescriptor, selectionSorter, randomSelection);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getVariableDescriptor().getVariableName() + ")";
    }

}
