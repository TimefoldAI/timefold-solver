package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadingSolution;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidField;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidPiggyback;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidSource;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongMethod;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SingleCascadingUpdateShadowVariableListenerTest {

    @Test
    void requiredShadowVariableDependencies() {
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
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingInvalidSource::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidSource)")
                .withMessageContaining("has a @ShadowVariable annotated property (cascadeValue2)")
                .withMessageContaining("with sourceVariableName (cascadeValue) which cannot be used as source")
                .withMessageContaining(
                        "Shadow variables such as @CascadingUpdateShadowVariable are not allowed to be used as source")
                .withMessageContaining("Maybe check if cascadeValue is annotated with @CascadingUpdateShadowVariable");
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingInvalidPiggyback::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingInvalidPiggyback)")
                .withMessageContaining("has a @PiggybackShadowVariable annotated property (cascadeValue2)")
                .withMessageContaining(
                        "with refVariable (TestdataCascadingInvalidPiggyback.cascadeValue) that lacks a @ShadowVariable annotation");
    }

    @Test
    void updateAllNextValues() {
        var variableDescriptor = TestdataSingleCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataSingleCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from update next val1, inverse and previous element changes
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        // Called from update next val2, inverse and previous element changes
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isOne();
    }

    @Test
    void stopUpdateNextValues() {
        var variableDescriptor = TestdataSingleCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataSingleCascadingSolution.generateUninitializedSolution(3, 2);
        solution.getValueList().get(1).setCascadeValue(3);
        solution.getValueList().get(1).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setEntity(solution.getEntityList().get(0));
        solution.getValueList().get(2).setPrevious(solution.getValueList().get(1));
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 1);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isNull();
        // Stop on value2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isZero();
    }
}
