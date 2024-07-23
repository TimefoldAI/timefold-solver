package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingSolution;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidField;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongMethod;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SingleCascadingUpdateShadowVariableListenerTest {

    @Test
    void requiredShadowVariableDependencies() {
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingWrongSource::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but the shadow variable \"bad\" cannot be found")
                .withMessageContaining(
                        "Maybe update sourceVariableName to an existing shadow variable in the entity class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingWrongMethod::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongMethod)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValueReturnType)")
                .withMessageContaining("but the method \"badUpdateCascadeValueWithReturnType\" cannot be found");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingInvalidField::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidField)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("but the method \"value\" cannot be found");
    }

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingSolution> variableDescriptor =
                TestdataSingleCascadingEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingSolution solution = TestdataSingleCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
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
        GenuineVariableDescriptor<TestdataSingleCascadingSolution> variableDescriptor =
                TestdataSingleCascadingEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingSolution solution = TestdataSingleCascadingSolution.generateUninitializedSolution(3, 2);
        solution.getValueList().get(1).setNext(solution.getValueList().get(2));
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingEntity entity = solution.getEntityList().get(0);
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
        GenuineVariableDescriptor<TestdataSingleCascadingSolution> variableDescriptor =
                TestdataSingleCascadingEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingSolution solution = TestdataSingleCascadingSolution.generateUninitializedSolution(3, 2);
        solution.getValueList().get(1).setCascadeValue(3);
        solution.getValueList().get(1).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setPrevious(solution.getValueList().get(1));
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingEntity entity = solution.getEntityList().get(0);
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
}
