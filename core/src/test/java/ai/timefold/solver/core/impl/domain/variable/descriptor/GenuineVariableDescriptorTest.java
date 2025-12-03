package ai.timefold.solver.core.impl.domain.variable.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;

import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.TestdataEntityProvidingWithParameterSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.parameter.TestdataInvalidCountEntityProvidingWithParameterEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.solution.invalid.parameter.TestdataInvalidTypeEntityProvidingWithParameterEntity;

import org.junit.jupiter.api.Test;

class GenuineVariableDescriptorTest {

    @Test
    void isReinitializable() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        assertThat(variableDescriptor.isReinitializable(new TestdataEntity("a", new TestdataValue()))).isFalse();
        assertThat(variableDescriptor.isReinitializable(new TestdataEntity("b", null))).isTrue();
    }

    @Test
    void isReinitializable_allowsUnassigned() {
        var variableDescriptor = TestdataAllowsUnassignedEntity.buildVariableDescriptorForValue();
        assertThat(variableDescriptor.isReinitializable(new TestdataAllowsUnassignedEntity("a", new TestdataValue())))
                .isFalse();
        assertThat(variableDescriptor.isReinitializable(new TestdataAllowsUnassignedEntity("b", null))).isTrue();
    }

    @Test
    void isReinitializable_list() {
        var variableDescriptor = TestdataListEntity.buildVariableDescriptorForValueList();
        assertThat(variableDescriptor.isReinitializable(new TestdataListEntity("a", new TestdataListValue()))).isFalse();
        assertThat(variableDescriptor.isReinitializable(new TestdataListEntity("b", new ArrayList<>()))).isFalse();
    }

    @Test
    void invalidReadMethodWithParameter() {
        assertThatCode(TestdataInvalidTypeEntityProvidingWithParameterEntity::buildVariableDescriptorForValueRange)
                .hasMessageContaining(
                        "The parameter type (ai.timefold.solver.core.testdomain.TestdataSolution) of the method (getValueRange) must match the solution (TestdataInvalidTypeEntityProvidingWithParameterSolution).");
        assertThatCode(TestdataInvalidCountEntityProvidingWithParameterEntity::buildVariableDescriptorForValueRange)
                .hasMessageContaining("The readMethod")
                .hasMessageContaining("with a ValueRangeProvider annotation must have only one parameter");
    }

    @Test
    void valueRangeDescriptorWithSolution() {
        assertThatCode(TestdataEntityProvidingWithParameterSolution::buildSolutionDescriptor)
                .doesNotThrowAnyException();
    }
}
