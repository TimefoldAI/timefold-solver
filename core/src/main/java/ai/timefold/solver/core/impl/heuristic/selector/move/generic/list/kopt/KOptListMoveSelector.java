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

    /**
     * A provable upper bound of distinct selections: the sum over k of n^k * 2^k * (2k - 1)!!,
     * where n is the number of values.
     * A k-opt move is a sequence of 2k removed edge endpoints (n choices for each odd one, 2 neighbors for each even one)
     * plus a matching of those 2k endpoints into added edges ((2k - 1)!! possibilities).
     * This counts degenerate and patched moves too, so it is much larger than the number of pure k-opt reconnections.
     */
    @Override
    public long getSize() {
        // Each removed edge: n choices for its first endpoint and 2 neighbors for its second.
        var endpointChoices = MathUtils.saturatingMultiply(valueSelector.getSize(), 2L);
        var total = 0L;
        for (var k = minK; k <= maxK; k++) {
            var kOptSize = 1L;
            for (var i = 1; i <= k; i++) {
                kOptSize = MathUtils.saturatingMultiply(kOptSize, endpointChoices);
                kOptSize = MathUtils.saturatingMultiply(kOptSize, 2L * i - 1);
            }
            total = MathUtils.saturatingAdd(total, kOptSize);
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
