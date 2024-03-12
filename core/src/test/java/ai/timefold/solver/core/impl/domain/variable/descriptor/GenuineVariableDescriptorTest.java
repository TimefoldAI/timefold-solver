package ai.timefold.solver.core.impl.domain.variable.descriptor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.allows_unassigned.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class GenuineVariableDescriptorTest {

    @Test
    void isReinitializable() {
        GenuineVariableDescriptor<?> variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        assertThat(variableDescriptor.isReinitializable(new TestdataEntity("a", new TestdataValue()))).isFalse();
        assertThat(variableDescriptor.isReinitializable(new TestdataEntity("b", null))).isTrue();
    }

    @Test
    void isReinitializable_allowsUnassigned() {
        GenuineVariableDescriptor<?> variableDescriptor = TestdataAllowsUnassignedEntity.buildVariableDescriptorForValue();
        assertThat(variableDescriptor.isReinitializable(new TestdataAllowsUnassignedEntity("a", new TestdataValue())))
                .isFalse();
        assertThat(variableDescriptor.isReinitializable(new TestdataAllowsUnassignedEntity("b", null))).isTrue();
    }

    @Test
    void isReinitializable_list() {
        GenuineVariableDescriptor<?> variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        assertThat(variableDescriptor.isReinitializable(new TestdataListEntity("a", new TestdataListValue()))).isFalse();
        assertThat(variableDescriptor.isReinitializable(new TestdataListEntity("b", new ArrayList<>()))).isFalse();
    }
}
