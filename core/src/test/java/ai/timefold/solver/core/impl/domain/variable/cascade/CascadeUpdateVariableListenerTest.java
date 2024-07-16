package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeSolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingInverseValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingNextValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingPreviousValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeWrongMethod;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class CascadeUpdateVariableListenerTest {

    @Test
    void requiredShadowVariableDependencies() {
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeMissingInverseValue::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingInverseValue)")
                .withMessageContaining("has an @CascadeUpdateElementShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but has no @InverseRelationShadowVariable shadow variable defined.");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeMissingPreviousValue::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingPreviousValue)")
                .withMessageContaining("has an @CascadeUpdateElementShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but has no @PreviousElementShadowVariable shadow variable defined");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeMissingNextValue::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeMissingNextValue)")
                .withMessageContaining("has an @CascadeUpdateElementShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but has no @NextElementShadowVariable shadow variable defined");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadeWrongMethod::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeWrongMethod)")
                .withMessageContaining(
                        "has an @CascadeUpdateElementShadowVariable annotated property (badUpdateCascadeValueWithReturnType)")
                .withMessageContaining(
                        "with sourceMethodName (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeWrongMethod), ")
                .withMessageContaining("but the method has not been found in the entityClass");
    }

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataCascadeSolution> variableDescriptor =
                TestdataCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataCascadeValue val1 = new TestdataCascadeValue(1);
        TestdataCascadeValue val2 = new TestdataCascadeValue(2);
        TestdataCascadeValue val3 = new TestdataCascadeValue(3);
        TestdataCascadeEntity a = new TestdataCascadeEntity("a");
        TestdataCascadeEntity b = new TestdataCascadeEntity("b");

        TestdataCascadeSolution solution = new TestdataCascadeSolution();
        solution.setEntityList(Arrays.asList(a, b));
        solution.setValueList(Arrays.asList(val1, val2, val3));
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeListVariableChanged(a, "valueList", 0, 3);
        a.setValueList(List.of(val1, val2, val3));
        scoreDirector.afterListVariableChanged(a, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(a.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(a.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);
        
        assertThat(a.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from update next val1, inverse and previous element changes
        assertThat(a.getValueList().get(1).getNumberOfCalls()).isEqualTo(3);
        
        assertThat(a.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        // Called from update next val2, inverse and previous element changes
        assertThat(a.getValueList().get(2).getNumberOfCalls()).isEqualTo(3);
    }

    @Test
    void updateOnlyMiddleValue() {
        GenuineVariableDescriptor<TestdataCascadeSolution> variableDescriptor =
                TestdataCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataCascadeValue val1 = new TestdataCascadeValue(1);
        TestdataCascadeValue val2 = new TestdataCascadeValue(2);
        TestdataCascadeValue val3 = new TestdataCascadeValue(3);
        val2.setNext(val3);
        TestdataCascadeEntity a = new TestdataCascadeEntity("a");
        TestdataCascadeEntity b = new TestdataCascadeEntity("b");

        TestdataCascadeSolution solution = new TestdataCascadeSolution();
        solution.setEntityList(Arrays.asList(a, b));
        solution.setValueList(Arrays.asList(val1, val2, val3));
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeListVariableChanged(a, "valueList", 1, 1);
        a.setValueList(List.of(val1, val2, val3));
        scoreDirector.afterListVariableChanged(a, "valueList", 1, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(a.getValueList().get(0).getNumberOfCalls()).isZero();

        assertThat(a.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from previous element change
        assertThat(a.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

        assertThat(a.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        // Called from update next val2
        assertThat(a.getValueList().get(2).getNumberOfCalls()).isEqualTo(1);
    }


    @Test
    void stopUpdateNextValues() {
        GenuineVariableDescriptor<TestdataCascadeSolution> variableDescriptor =
                TestdataCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataCascadeValue val1 = new TestdataCascadeValue(1);
        TestdataCascadeValue val2 = new TestdataCascadeValue(2);
        val2.setCascadeValue(3);
        TestdataCascadeValue val3 = new TestdataCascadeValue(3);
        TestdataCascadeEntity a = new TestdataCascadeEntity("a");
        val2.setEntity(a);
        val3.setEntity(a);
        val3.setPrevious(val2);
        TestdataCascadeEntity b = new TestdataCascadeEntity("b");

        TestdataCascadeSolution solution = new TestdataCascadeSolution();
        solution.setEntityList(Arrays.asList(a, b));
        solution.setValueList(Arrays.asList(val1, val2, val3));
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeListVariableChanged(a, "valueList", 0, 1);
        a.setValueList(List.of(val1, val2, val3));
        scoreDirector.afterListVariableChanged(a, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(a.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(a.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);
        
        assertThat(a.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from update next val1 and previous element change
        assertThat(a.getValueList().get(1).getNumberOfCalls()).isEqualTo(2);
        
        assertThat(a.getValueList().get(2).getCascadeValue()).isNull();
        // Stop on value2
        assertThat(a.getValueList().get(2).getNumberOfCalls()).isZero();
    }

}
