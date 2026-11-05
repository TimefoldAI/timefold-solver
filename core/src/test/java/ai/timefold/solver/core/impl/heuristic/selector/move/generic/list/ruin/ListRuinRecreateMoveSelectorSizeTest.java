package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ruin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class ListRuinRecreateMoveSelectorSizeTest {

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void sizeCountsOrderedPicks() {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        var valueSelector = (IterableValueSelector<TestdataListSolution>) mock(IterableValueSelector.class);
        when(valueSelector.getVariableDescriptor()).thenReturn((GenuineVariableDescriptor) variableDescriptor);
        when(valueSelector.getSize()).thenReturn(25L);
        // Ordered picks of 1 or 2 out of 25: 25 + 25 * 24.
        var selector = new ListRuinRecreateMoveSelector<>(valueSelector, variableDescriptor, null, count -> 1, count -> 2);
        assertThat(selector.getSize()).isEqualTo(625L);
        var hugeSelector =
                new ListRuinRecreateMoveSelector<>(valueSelector, variableDescriptor, null, count -> 20, count -> 20);
        assertThat(hugeSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

}
