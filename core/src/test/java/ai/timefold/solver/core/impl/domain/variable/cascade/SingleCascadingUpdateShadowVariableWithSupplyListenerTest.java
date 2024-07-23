package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply.TestdataSingleCascadingWithSupplyEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply.TestdataSingleCascadingWithSupplySolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.suply.TestdataSingleCascadingWithSupplyValue;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongMethod;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSource;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SingleCascadingUpdateShadowVariableWithSupplyListenerTest {

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
    }

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingWithSupplySolution> variableDescriptor =
                TestdataSingleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingWithSupplySolution solution =
                TestdataSingleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
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
        GenuineVariableDescriptor<TestdataSingleCascadingWithSupplySolution> variableDescriptor =
                TestdataSingleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingWithSupplySolution solution =
                TestdataSingleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList().subList(0, 2));
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
        scoreDirector.triggerVariableListeners();
        solution.getValueList().forEach(TestdataSingleCascadingWithSupplyValue::reset);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
        entity.setValueList(
                List.of(solution.getValueList().get(0), solution.getValueList().get(2), solution.getValueList().get(1)));
        scoreDirector.afterListVariableChanged(entity, "valueList", 1, 2);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isZero();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(4);
        // Called from previous and inverse element change
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isEqualTo(2);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        // Called from update next val
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isEqualTo(2);
    }

    @Test
    void stopUpdateNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingWithSupplySolution> variableDescriptor =
                TestdataSingleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingWithSupplySolution solution =
                TestdataSingleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList().subList(0, 2));
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
        scoreDirector.triggerVariableListeners();
        solution.getValueList().forEach(TestdataSingleCascadingWithSupplyValue::reset);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(
                List.of(solution.getValueList().get(2), solution.getValueList().get(0), solution.getValueList().get(1)));
        solution.getValueList().get(2).setCascadeValue(4);
        solution.getValueList().get(0).setCascadeValue(2);
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(0).getFirstNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
        // Called from update next val1 and previous element change
        assertThat(entity.getValueList().get(1).getFirstNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        // Stop on value2
        assertThat(entity.getValueList().get(2).getFirstNumberOfCalls()).isZero();
    }
}
