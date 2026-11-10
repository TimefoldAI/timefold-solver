package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector.filterPinnedListPlanningVariableValuesWithIndex;

import java.util.Iterator;
import java.util.function.Supplier;

import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
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
        this.originSelector = createEffectiveValueSelector(originSelector, this::getListVariableState);
        this.valueSelector = createEffectiveValueSelector(valueSelector, this::getListVariableState);
        this.minK = minK;
        this.maxK = maxK;
        this.pickedKDistribution = pickedKDistribution;

        phaseLifecycleSupport.addEventListener(this.originSelector);
        phaseLifecycleSupport.addEventListener(this.valueSelector);
    }

    private IterableValueSelector<Solution_> createEffectiveValueSelector(
            IterableValueSelector<Solution_> iterableValueSelector,
            Supplier<ListVariableState<Solution_, Object, Object>> listVariableStateSupplier) {
        var filteredValueSelector =
                filterPinnedListPlanningVariableValuesWithIndex(iterableValueSelector, listVariableStateSupplier);
        return FilteringValueSelector.ofAssigned(filteredValueSelector, listVariableStateSupplier);
    }

    @Override
    public long getSize() {
        var valueSelectorSize = valueSelector.getSize();
        if (valueSelectorSize > Integer.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        var total = 0L;
        // A k-opt needs more than k values.
        for (var i = minK; i <= maxK && i < valueSelectorSize; i++) {
            try {
                // A tour with n nodes has n - 1 edges, and a k-opt removes k of them.
                var edgeChoices = MathUtils.binomialCoefficient((int) (valueSelectorSize - 1), i);
                total = Math.addExact(total, Math.multiplyExact(KOptUtils.getPureKOptMoveTypes(i), edgeChoices));
            } catch (ArithmeticException e) {
                // binomialCoefficient() and the exact arithmetic throw on overflow.
                return Long.MAX_VALUE;
            }
        }
        return total;
    }

    @Override
    public Iterator<Move<Solution_>> iterator() {
        return new KOptListMoveIterator<>(workingRandom, listVariableDescriptor, listVariableState,
                originSelector, valueSelector, minK, maxK, pickedKDistribution);
    }

    @Override
    public boolean isNeverEnding() {
        return true;
    }
}
