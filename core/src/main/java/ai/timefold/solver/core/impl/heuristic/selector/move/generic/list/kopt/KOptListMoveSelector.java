package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.GenericMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.apache.commons.math3.util.CombinatoricsUtils;

final class KOptListMoveSelector<Solution_> extends GenericMoveSelector<Solution_> {

    private final ListVariableDescriptor<Solution_> listVariableDescriptor;

    private final EntityIndependentValueSelector<Solution_> originSelector;
    private final EntityIndependentValueSelector<Solution_> valueSelector;
    private final int minK;
    private final int maxK;

    private final int[] pickedKDistribution;

    private ListVariableStateSupply<Solution_> listVariableStateSupply;
    private EntityIndependentValueSelector<Solution_> effectiveOriginSelector;
    private EntityIndependentValueSelector<Solution_> effectiveValueSelector;

    public KOptListMoveSelector(ListVariableDescriptor<Solution_> listVariableDescriptor,
            EntityIndependentValueSelector<Solution_> originSelector, EntityIndependentValueSelector<Solution_> valueSelector,
            int minK, int maxK, int[] pickedKDistribution) {
        this.listVariableDescriptor = listVariableDescriptor;
        this.originSelector = originSelector;
        this.valueSelector = valueSelector;
        this.minK = minK;
        this.maxK = maxK;
        this.pickedKDistribution = pickedKDistribution;
        phaseLifecycleSupport.addEventListener(originSelector);
        phaseLifecycleSupport.addEventListener(valueSelector);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        var supplyManager = solverScope.getScoreDirector().getSupplyManager();
        listVariableStateSupply = supplyManager.demand(listVariableDescriptor.getStateDemand());
        effectiveOriginSelector = createEffectiveValueSelector(originSelector, listVariableStateSupply);
        effectiveValueSelector = createEffectiveValueSelector(valueSelector, listVariableStateSupply);
    }

    private EntityIndependentValueSelector<Solution_> createEffectiveValueSelector(
            EntityIndependentValueSelector<Solution_> entityIndependentValueSelector,
            ListVariableStateSupply<Solution_> listVariableStateSupply) {
        var filteredValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(entityIndependentValueSelector, listVariableStateSupply);
        return FilteringValueSelector.ofAssigned(filteredValueSelector, listVariableStateSupply);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        listVariableStateSupply = null;
        effectiveOriginSelector = null;
        effectiveValueSelector = null;
    }

    @Override
    public long getSize() {
        long total = 0;
        long valueSelectorSize = effectiveValueSelector.getSize();
        for (int i = minK; i < Math.min(valueSelectorSize, maxK); i++) {
            if (valueSelectorSize > i) { // need more than k nodes in order to perform a k-opt
                long kOptMoveTypes = KOptUtils.getPureKOptMoveTypes(i);

                // A tour with n nodes have n - 1 edges
                // And we chose k of them to remove in a k-opt
                final long edgeChoices;
                if (valueSelectorSize <= Integer.MAX_VALUE) {
                    edgeChoices = CombinatoricsUtils.binomialCoefficient((int) (valueSelectorSize - 1), i);
                } else {
                    edgeChoices = Long.MAX_VALUE;
                }
                total += kOptMoveTypes * edgeChoices;
            }
        }
        return total;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new KOptListMoveIterator<>(workingRandom, listVariableDescriptor, listVariableStateSupply,
                effectiveOriginSelector, effectiveValueSelector, minK, maxK, pickedKDistribution);
    }

    @Override
    public boolean isCountable() {
        return false;
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }
}
