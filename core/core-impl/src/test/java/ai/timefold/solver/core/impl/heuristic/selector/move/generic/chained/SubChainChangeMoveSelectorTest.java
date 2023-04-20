package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.DefaultSubChainSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.chained.SubChainSelector;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;

import org.junit.jupiter.api.Test;

class SubChainChangeMoveSelectorTest {

    @Test
    void differentValueDescriptorException() {
        SubChainSelector subChainSelector = mock(DefaultSubChainSelector.class);
        GenuineVariableDescriptor descriptor = TestdataEntity.buildVariableDescriptorForValue();
        when(subChainSelector.getVariableDescriptor()).thenReturn(descriptor);
        EntityIndependentValueSelector valueSelector = mock(EntityIndependentValueSelector.class);
        GenuineVariableDescriptor otherDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        when(valueSelector.getVariableDescriptor()).thenReturn(otherDescriptor);
        assertThatIllegalStateException().isThrownBy(
                () -> new SubChainChangeMoveSelector(subChainSelector, valueSelector, true, true));
    }

    @Test
    void determinedSelectionWithNeverEndingChainSelector() {
        SubChainSelector subChainSelector = mock(DefaultSubChainSelector.class);
        when(subChainSelector.isNeverEnding()).thenReturn(true);
        GenuineVariableDescriptor descriptor = TestdataEntity.buildVariableDescriptorForValue();
        when(subChainSelector.getVariableDescriptor()).thenReturn(descriptor);
        EntityIndependentValueSelector valueSelector = mock(EntityIndependentValueSelector.class);
        when(valueSelector.getVariableDescriptor()).thenReturn(descriptor);
        assertThatIllegalStateException().isThrownBy(
                () -> new SubChainChangeMoveSelector(subChainSelector, valueSelector, false, true));
    }

    @Test
    void determinedSelectionWithNeverEndingValueSelector() {
        SubChainSelector subChainSelector = mock(DefaultSubChainSelector.class);
        GenuineVariableDescriptor descriptor = TestdataEntity.buildVariableDescriptorForValue();
        when(subChainSelector.getVariableDescriptor()).thenReturn(descriptor);
        EntityIndependentValueSelector valueSelector = mock(EntityIndependentValueSelector.class);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.getVariableDescriptor()).thenReturn(descriptor);
        assertThatIllegalStateException().isThrownBy(
                () -> new SubChainChangeMoveSelector(subChainSelector, valueSelector, false, true));
    }

    @Test
    void isCountable() {
        SubChainSelector subChainSelector = mock(DefaultSubChainSelector.class);
        GenuineVariableDescriptor descriptor = TestdataEntity.buildVariableDescriptorForValue();
        when(subChainSelector.getVariableDescriptor()).thenReturn(descriptor);
        EntityIndependentValueSelector valueSelector = mock(EntityIndependentValueSelector.class);
        when(valueSelector.getVariableDescriptor()).thenReturn(descriptor);
        SubChainChangeMoveSelector testedSelector = new SubChainChangeMoveSelector(subChainSelector, valueSelector, true, true);

        when(subChainSelector.isCountable()).thenReturn(false);
        when(valueSelector.isCountable()).thenReturn(true);
        assertThat(testedSelector.isCountable()).isFalse();

        when(subChainSelector.isCountable()).thenReturn(true);
        when(valueSelector.isCountable()).thenReturn(false);
        assertThat(testedSelector.isCountable()).isFalse();

        when(subChainSelector.isCountable()).thenReturn(true);
        when(valueSelector.isCountable()).thenReturn(true);
        assertThat(testedSelector.isCountable()).isTrue();

        when(subChainSelector.isCountable()).thenReturn(false);
        when(valueSelector.isCountable()).thenReturn(false);
        assertThat(testedSelector.isCountable()).isFalse();
    }

    @Test
    void getSize() {
        SubChainSelector subChainSelector = mock(DefaultSubChainSelector.class);
        GenuineVariableDescriptor descriptor = TestdataEntity.buildVariableDescriptorForValue();
        when(subChainSelector.getVariableDescriptor()).thenReturn(descriptor);
        EntityIndependentValueSelector valueSelector = mock(EntityIndependentValueSelector.class);
        when(valueSelector.getVariableDescriptor()).thenReturn(descriptor);
        SubChainChangeMoveSelector testedSelector = new SubChainChangeMoveSelector(subChainSelector, valueSelector, true, true);

        when(subChainSelector.getSize()).thenReturn(1L);
        when(valueSelector.getSize()).thenReturn(2L);
        assertThat(testedSelector.getSize()).isEqualTo(2);

        when(subChainSelector.getSize()).thenReturn(100L);
        when(valueSelector.getSize()).thenReturn(200L);
        assertThat(testedSelector.getSize()).isEqualTo(20000);
    }

}
