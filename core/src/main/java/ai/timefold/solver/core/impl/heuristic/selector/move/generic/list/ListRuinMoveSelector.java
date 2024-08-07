package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;

import java.util.Iterator;
import java.util.function.ToLongFunction;

import ai.timefold.solver.core.impl.constructionheuristic.RuinRecreateConstructionHeuristicPhase;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.apache.commons.math3.util.CombinatoricsUtils;

public class ListRuinMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {
    protected final EntityIndependentValueSelector<Solution_> valueSelector;
    protected final ListVariableDescriptor<Solution_> listVariableDescriptor;
    protected final RuinRecreateConstructionHeuristicPhase<Solution_> constructionHeuristicPhase;

    protected final ToLongFunction<Long> minimumSelectedCountSupplier;
    protected final ToLongFunction<Long> maximumSelectedCountSupplier;

    protected SolverScope<Solution_> solverScope;
    protected ListVariableStateSupply<Solution_> listVariableStateSupply;

    public ListRuinMoveSelector(EntityIndependentValueSelector<Solution_> valueSelector,
            ListVariableDescriptor<Solution_> listVariableDescriptor,
            RuinRecreateConstructionHeuristicPhase<Solution_> constructionHeuristicPhase,
            ToLongFunction<Long> minimumSelectedCountSupplier,
            ToLongFunction<Long> maximumSelectedCountSupplier) {
        super();
        this.valueSelector = valueSelector;
        this.listVariableDescriptor = listVariableDescriptor;
        this.constructionHeuristicPhase = constructionHeuristicPhase;
        this.minimumSelectedCountSupplier = minimumSelectedCountSupplier;
        this.maximumSelectedCountSupplier = maximumSelectedCountSupplier;

        phaseLifecycleSupport.addEventListener(valueSelector);
    }

    @Override
    public long getSize() {
        long totalSize = 0;
        long entityCount = valueSelector.getSize();
        var minimumSelectedCount = minimumSelectedCountSupplier.applyAsLong(entityCount);
        var maximumSelectedCount = maximumSelectedCountSupplier.applyAsLong(entityCount);
        for (long selectedCount = minimumSelectedCount; selectedCount <= maximumSelectedCount; selectedCount++) {
            // Order is significant, and each entity can only be picked once
            totalSize += CombinatoricsUtils.factorial((int) entityCount) / CombinatoricsUtils.factorial((int) selectedCount);
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
        constructionHeuristicPhase.setSolver(solverScope.getSolver());
        this.solverScope = solverScope;
        this.listVariableStateSupply =
                solverScope.getScoreDirector().getSupplyManager().demand(listVariableDescriptor.getStateDemand());
        this.workingRandom = solverScope.getWorkingRandom();
    }

    private EntityIndependentValueSelector<Solution_> filterNotAssignedValues(
            EntityIndependentValueSelector<Solution_> entityIndependentValueSelector,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        if (!listVariableDescriptor.allowsUnassignedValues()) {
            return entityIndependentValueSelector;
        }
        // We need to filter out unassigned vars.
        return (EntityIndependentValueSelector<Solution_>) FilteringValueSelector.of(entityIndependentValueSelector,
                (scoreDirector, selection) -> {
                    if (listVariableStateSupply.getUnassignedCount() == 0) {
                        return true;
                    }
                    return listVariableStateSupply.isAssigned(selection);
                });
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        var assignedValueSelector = filterNotAssignedValues(valueSelector, listVariableStateSupply);
        return new ListRuinMoveIterator<>(assignedValueSelector,
                listVariableDescriptor,
                constructionHeuristicPhase,
                solverScope,
                listVariableStateSupply,
                minimumSelectedCountSupplier.applyAsLong(assignedValueSelector.getSize()),
                maximumSelectedCountSupplier.applyAsLong(assignedValueSelector.getSize()),
                workingRandom);
    }
}
