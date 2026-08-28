package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.AbstractGenericListMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.util.MathUtils;
import ai.timefold.solver.core.preview.api.move.Move;

final class KOptListMoveSelector<Solution_> extends AbstractGenericListMoveSelector<Solution_> {

    private final IterableValueSelector<Solution_> originSelector;
    private final IterableValueSelector<Solution_> valueSelector;
    private final int minK;
    private final int maxK;

    private final int[] pickedKDistribution;

    public KOptListMoveSelector(ListVariableDescriptor<Solution_> listVariableDescriptor,
            IterableValueSelector<Solution_> originSelector, IterableValueSelector<Solution_> valueSelector,
            int minK, int maxK, int[] pickedKDistribution) {
        super(listVariableDescriptor);
        this.originSelector = createEffectiveValueSelector(originSelector, this::getListVariableStateSupply);
        this.valueSelector = createEffectiveValueSelector(valueSelector, this::getListVariableStateSupply);
        this.minK = minK;
        this.maxK = maxK;
        this.pickedKDistribution = pickedKDistribution;

        phaseLifecycleSupport.addEventListener(this.originSelector);
        phaseLifecycleSupport.addEventListener(this.valueSelector);
    }

    private IterableValueSelector<Solution_> createEffectiveValueSelector(
            IterableValueSelector<Solution_> iterableValueSelector,
            Supplier<ListVariableStateSupply<Solution_, Object, Object>> listVariableStateSupplier) {
        var filteredValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(iterableValueSelector, listVariableStateSupplier);
        return FilteringValueSelector.ofAssigned(filteredValueSelector, listVariableStateSupplier);
    }

    @Override
    public long getSize() {
        long total = 0;
        long valueSelectorSize = valueSelector.getSize();
        for (int i = minK; i < Math.min(valueSelectorSize, maxK); i++) {
            if (valueSelectorSize > i) { // need more than k nodes in order to perform a k-opt
                long kOptMoveTypes = KOptUtils.getPureKOptMoveTypes(i);

                // A tour with n nodes have n - 1 edges
                // And we chose k of them to remove in a k-opt
                final long edgeChoices;
                if (valueSelectorSize <= Integer.MAX_VALUE) {
                    edgeChoices = MathUtils.binomialCoefficient((int) (valueSelectorSize - 1), i);
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
                originSelector, valueSelector, minK, maxK, pickedKDistribution);
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }
}
