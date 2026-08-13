package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupplyHolder;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.preview.api.domain.metamodel.UnassignedElement;
import ai.timefold.solver.core.preview.api.move.Move;

public class ListChangeMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final IterableValueSelector<Solution_> sourceValueSelector;
    private final DestinationSelector<Solution_> destinationSelector;
    private final boolean randomSelection;

    private final ListVariableStateSupplyHolder<Solution_> listVariableStateSupplyHolder;

    public ListChangeMoveSelector(IterableValueSelector<Solution_> sourceValueSelector,
            DestinationSelector<Solution_> destinationSelector, boolean randomSelection) {
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) sourceValueSelector.getVariableDescriptor();
        this.listVariableStateSupplyHolder = new ListVariableStateSupplyHolder<>(listVariableDescriptor);
        this.sourceValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(sourceValueSelector, listVariableStateSupplyHolder::get);
        this.destinationSelector = destinationSelector;
        this.randomSelection = randomSelection;
        phaseLifecycleSupport.addEventListener(this.sourceValueSelector);
        phaseLifecycleSupport.addEventListener(this.destinationSelector);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        // The phase may operate in a different environment mode, which uses a new score director.
        // We must ensure that the list variable state supply remains up to date.
        listVariableStateSupplyHolder.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        listVariableStateSupplyHolder.phaseEnded(phaseScope);
    }

    public static <Solution_> IterableValueSelector<Solution_> filterPinnedListPlanningVariableValuesWithIndex(
            IterableValueSelector<Solution_> sourceValueSelector,
            Supplier<ListVariableStateSupply<Solution_, Object, Object>> listVariableStateSupplier) {
        var listVariableDescriptor = (ListVariableDescriptor<Solution_>) sourceValueSelector.getVariableDescriptor();
        var supportsPinning = listVariableDescriptor.supportsPinning();
        if (!supportsPinning) {
            // Don't incur the overhead of filtering values if there is no pinning support.
            return sourceValueSelector;
        }
        return (IterableValueSelector<Solution_>) FilteringValueSelector.of(sourceValueSelector,
                (scoreDirector, selection) -> {
                    var listVariableStateSupply = listVariableStateSupplier.get();
                    var elementPosition = listVariableStateSupply.getElementPosition(selection);
                    if (elementPosition instanceof UnassignedElement) {
                        return true;
                    }
                    var elementDestination = elementPosition.ensureAssigned();
                    var entity = elementDestination.entity();
                    return !listVariableDescriptor.isElementPinned(scoreDirector.getWorkingSolution(), entity,
                            elementDestination.index());
                });
    }

    @Override
    public long getSize() {
        return sourceValueSelector.getSize() * destinationSelector.getSize();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        if (randomSelection) {
            return new RandomListChangeIterator<>(
                    listVariableStateSupplyHolder.get(),
                    sourceValueSelector,
                    destinationSelector);
        } else {
            return new OriginalListChangeIterator<>(
                    listVariableStateSupplyHolder.get(),
                    sourceValueSelector,
                    destinationSelector);
        }
    }

    @Override
    public boolean isNeverEnding() {
        return randomSelection || sourceValueSelector.isNeverEnding() || destinationSelector.isNeverEnding();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + sourceValueSelector + ", " + destinationSelector + ")";
    }
}
