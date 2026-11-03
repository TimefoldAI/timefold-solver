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
            "10, 2, 2, 36", // 1 * C(9, 2)
            "10, 3, 3, 336", // 4 * C(9, 3)
            "10, 2, 3, 372",
            "3, 3, 3, 0", // A 3-opt needs more than 3 values.
            "4, 2, 3, 7" // 1 * C(3, 2) + 4 * C(3, 3)
    })
    void sizeIncludesMaximumK(long valueCount, int minK, int maxK, long expectedSize) {
        assertThat(buildSelector(valueCount, minK, maxK).getSize()).isEqualTo(expectedSize);
    }

}
