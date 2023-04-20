package ai.timefold.solver.core.impl.domain.variable.descriptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.valuerange.TestdataListEntityWithArrayValueRange;

import org.junit.jupiter.api.Test;

class ListVariableDescriptorTest {

    @Test
    void elementType() {
        assertThat(TestdataListEntity.buildVariableDescriptorForValueList().getElementType())
                .isEqualTo(TestdataListValue.class);
    }

    @Test
    void acceptsValueType() {
        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor =
                TestdataListEntity.buildVariableDescriptorForValueList();

        assertThat(listVariableDescriptor.acceptsValueType(TestdataListValue.class)).isTrue();
        assertThat(listVariableDescriptor.acceptsValueType(List.class)).isFalse();
    }

    @Test
    void buildDescriptorWithArrayValueRange() {
        assertThatCode(TestdataListEntityWithArrayValueRange::buildVariableDescriptorForValueList)
                .doesNotThrowAnyException();
    }

    @Test
    void requiresEntityIndependentValueRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(TestdataListEntityProvidingSolution::buildSolutionDescriptor)
                .withMessageContaining("is not supported with a list variable");
    }
}
