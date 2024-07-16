package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataMultipleCascadeEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataMultipleCascadeSolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadeEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadeSolution;
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
                        "with targetMethodName (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadeWrongMethod), ")
                .withMessageContaining("but the method has not been found in the entityClass");
    }

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadeSolution> variableDescriptor =
                TestdataSingleCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadeSolution solution = TestdataSingleCascadeSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadeEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 3);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(0).getCascadeValueReturnType()).isEqualTo(3);
        assertThat(entity.getValueList().get(0).getSecondNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from update next val1, inverse and previous element changes
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isEqualTo(3);

        assertThat(entity.getValueList().get(1).getCascadeValueReturnType()).isEqualTo(4);
        // Called from update next val1, inverse and previous element changes
        assertThat(entity.getValueList().get(1).getSecondNumberOfCalls()).isEqualTo(3);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        // Called from update next val2, inverse and previous element changes
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isEqualTo(3);

        assertThat(entity.getValueList().get(2).getCascadeValueReturnType()).isEqualTo(5);
        // Called from update next val2, inverse and previous element changes
        assertThat(entity.getValueList().get(2).getSecondNumberOfCalls()).isEqualTo(3);
    }

    @Test
    void updateOnlyMiddleValue() {
        GenuineVariableDescriptor<TestdataSingleCascadeSolution> variableDescriptor =
                TestdataSingleCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadeSolution solution = TestdataSingleCascadeSolution.generateUninitializedSolution(3, 2);
        solution.getValueList().get(1).setNext(solution.getValueList().get(2));
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadeEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 1, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isZero();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from previous element change
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        // Called from update next val2
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isEqualTo(1);
    }

    @Test
    void stopUpdateNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadeSolution> variableDescriptor =
                TestdataSingleCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadeSolution solution = TestdataSingleCascadeSolution.generateUninitializedSolution(3, 2);
        solution.getValueList().get(1).setCascadeValue(3);
        solution.getValueList().get(1).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setPrevious(solution.getValueList().get(1));
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadeEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 1);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from update next val1 and previous element change
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isEqualTo(2);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isNull();
        // Stop on value2
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isZero();
    }

    @Test
    void updateAllNextValuesWithMultipleVars() {
        GenuineVariableDescriptor<TestdataMultipleCascadeSolution> variableDescriptor =
                TestdataMultipleCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataMultipleCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataMultipleCascadeSolution solution = TestdataMultipleCascadeSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataMultipleCascadeEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 3);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(1);
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
        // Called from update next val1, inverse and previous element changes
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(3);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
        // Called from update next val2, inverse and previous element changes
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(3);
    }

    @Test
    void updateOnlyMiddleValueWithMultipleVars() {
        GenuineVariableDescriptor<TestdataMultipleCascadeSolution> variableDescriptor =
                TestdataMultipleCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataMultipleCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        { // Changing the first shadow var
            TestdataMultipleCascadeSolution solution = TestdataMultipleCascadeSolution.generateUninitializedSolution(3, 2);
            solution.getValueList().get(1).setSecondCascadeValue(3);
            solution.getValueList().get(1).setNext(solution.getValueList().get(2));
            scoreDirector.setWorkingSolution(solution);

            TestdataMultipleCascadeEntity entity = solution.getEntityList().get(0);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
            entity.setValueList(solution.getValueList());
            scoreDirector.afterListVariableChanged(entity, "valueList", 1, 1);
            scoreDirector.triggerVariableListeners();

            assertThat(entity.getValueList().get(0).getNumberOfCalls()).isZero();

            assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
            assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
            // Called from previous element change
            assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

            assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
            assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
            // Called from update next val2
            assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(1);
        }

        { // Changing the second shadow var
            TestdataMultipleCascadeSolution solution = TestdataMultipleCascadeSolution.generateUninitializedSolution(3, 2);
            solution.getValueList().get(1).setCascadeValue(2);
            solution.getValueList().get(1).setNext(solution.getValueList().get(2));
            scoreDirector.setWorkingSolution(solution);

            TestdataMultipleCascadeEntity entity = solution.getEntityList().get(0);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
            entity.setValueList(solution.getValueList());
            scoreDirector.afterListVariableChanged(entity, "valueList", 1, 1);
            scoreDirector.triggerVariableListeners();

            assertThat(entity.getValueList().get(0).getNumberOfCalls()).isZero();

            assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
            assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
            // Called from previous element change
            assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

            assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
            assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
            // Called from update next val2
            assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(1);
        }
    }

    @Test
    void stopUpdateNextValuesWithMultipleVars() {
        GenuineVariableDescriptor<TestdataMultipleCascadeSolution> variableDescriptor =
                TestdataMultipleCascadeEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataMultipleCascadeSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataMultipleCascadeSolution solution = TestdataMultipleCascadeSolution.generateUninitializedSolution(3, 2);
        solution.getValueList().get(1).setCascadeValue(2);
        solution.getValueList().get(1).setSecondCascadeValue(3);
        solution.getValueList().get(1).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setPrevious(solution.getValueList().get(1));
        scoreDirector.setWorkingSolution(solution);

        TestdataMultipleCascadeEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 1);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(1);
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
        // Called from update next val1 and previous element change
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(2);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isNull();
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isNull();
        // Stop on value2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isZero();
    }
}
