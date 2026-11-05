package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class KOptListMoveSelectorTest {

    private static KOptListMoveSelector<TestdataListSolution> buildSelector(long valueCount, int minK, int maxK) {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        return new KOptListMoveSelector<>(variableDescriptor, mockValueSelector(variableDescriptor, valueCount),
                mockValueSelector(variableDescriptor, valueCount), minK, maxK, new int[maxK - minK + 1]);
    }

    @SuppressWarnings("unchecked")
    private static IterableValueSelector<TestdataListSolution> mockValueSelector(
            GenuineVariableDescriptor<TestdataListSolution> variableDescriptor, long valueCount) {
        var valueSelector = (IterableValueSelector<TestdataListSolution>) mock(IterableValueSelector.class);
        when(valueSelector.getVariableDescriptor()).thenReturn(variableDescriptor);
        when(valueSelector.getSize()).thenReturn(valueCount);
        return valueSelector;
    }

    @ParameterizedTest
    @CsvSource({ "100000, 2, 5", "10000000, 2, 5", "3000000000, 2, 4", "19, 18, 18", "23, 22, 22" })
    void sizeSaturatesInsteadOfOverflowing(long valueCount, int minK, int maxK) {
        assertThat(buildSelector(valueCount, minK, maxK).getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 2, 2, 1200", // (10 * 2)^2 * 1 * 3
            "10, 3, 3, 120000", // (10 * 2)^3 * 1 * 3 * 5
            "10, 2, 3, 121200",
            "3, 3, 3, 3240", // Degenerate moves exist even when a 3-opt has only 3 values.
            "0, 2, 3, 0"
    })
    void sizeIsUpperBoundOfDistinctSelections(long valueCount, int minK, int maxK, long expectedSize) {
        assertThat(buildSelector(valueCount, minK, maxK).getSize()).isEqualTo(expectedSize);
    }

}
