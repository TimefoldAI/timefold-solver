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
    void sizeDoesNotOverflow() {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        var valueSelector = (IterableValueSelector<TestdataListSolution>) mock(IterableValueSelector.class);
        when(valueSelector.getVariableDescriptor()).thenReturn((GenuineVariableDescriptor) variableDescriptor);
        when(valueSelector.getSize()).thenReturn(25L);
        // 25! does not fit in a long, but 25! / 24! + 25! / 25! does.
        var selector = new ListRuinRecreateMoveSelector<>(valueSelector, variableDescriptor, null, count -> 24, count -> 25);
        assertThat(selector.getSize()).isEqualTo(26L);
        var hugeSelector =
                new ListRuinRecreateMoveSelector<>(valueSelector, variableDescriptor, null, count -> 5, count -> 5);
        assertThat(hugeSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

}
