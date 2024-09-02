package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import java.util.Iterator;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.CountSupplier;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.RuinRecreateConstructionHeuristicPhaseBuilder;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.apache.commons.math3.util.CombinatoricsUtils;

final class ListRuinRecreateMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final ListVariableDescriptor<Solution_> listVariableDescriptor;
    private final RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder;
    private final CountSupplier minimumSelectedCountSupplier;
    private final CountSupplier maximumSelectedCountSupplier;

    private SolverScope<Solution_> solverScope;
    private ListVariableStateSupply<Solution_> listVariableStateSupply;

    public ListRuinRecreateMoveSelector(EntityIndependentValueSelector<Solution_> valueSelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            RuinRecreateConstructionHeuristicPhaseBuilder<Solution_> constructionHeuristicPhaseBuilder,
            CountSupplier minimumSelectedCountSupplier, CountSupplier maximumSelectedCountSupplier) {
        super();
        this.valueSelector = FilteringValueSelector.ofAssigned(valueSelector, this::getListVariableStateSupply);
        this.listVariableDescriptor = listVariableDescriptor;
        this.constructionHeuristicPhaseBuilder = constructionHeuristicPhaseBuilder;
        this.minimumSelectedCountSupplier = minimumSelectedCountSupplier;
        this.maximumSelectedCountSupplier = maximumSelectedCountSupplier;

        phaseLifecycleSupport.addEventListener(this.valueSelector);
    }

    private ListVariableStateSupply<Solution_> getListVariableStateSupply() {
        return Objects.requireNonNull(listVariableStateSupply,
                "Impossible state: The listVariableStateSupply is not initialized yet.");
    }

    @Override
    public long getSize() {
        var totalSize = 0L;
        var valueCount = valueSelector.getSize();
        var minimumSelectedCount = minimumSelectedCountSupplier.applyAsInt(valueCount);
        var maximumSelectedCount = maximumSelectedCountSupplier.applyAsInt(valueCount);
        for (var selectedCount = minimumSelectedCount; selectedCount <= maximumSelectedCount; selectedCount++) {
            // Order is significant, and each entity can only be picked once
            totalSize += CombinatoricsUtils.factorial((int) valueCount) / CombinatoricsUtils.factorial(selectedCount);
        }
        return totalSize;
    }

    @Override
    public boolean isCountable() {
        return valueSelector.isCountable();
    }

    @Override
    public boolean isNeverEnding() {
        return valueSelector.isNeverEnding();
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        this.solverScope = solverScope;
        this.listVariableStateSupply = solverScope.getScoreDirector()
                .getSupplyManager()
                .demand(listVariableDescriptor.getStateDemand());
        this.workingRandom = solverScope.getWorkingRandom();
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        var valueSelectorSize = valueSelector.getSize();
        return new ListRuinRecreateMoveIterator<>(valueSelector, constructionHeuristicPhaseBuilder,
                solverScope, listVariableStateSupply,
                minimumSelectedCountSupplier.applyAsInt(valueSelectorSize),
                maximumSelectedCountSupplier.applyAsInt(valueSelectorSize),
                workingRandom);
    }
}
