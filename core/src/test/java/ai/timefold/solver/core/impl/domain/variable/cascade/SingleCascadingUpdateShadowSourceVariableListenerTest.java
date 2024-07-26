package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.souce.TestdataSingleCascadingSouceSolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.souce.TestdataSingleCascadingSourceEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingMultipleSources;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongEntity;
import ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSourceWithEntity;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class SingleCascadingUpdateShadowSourceVariableListenerTest {

    @Test
    void requiredShadowVariableDependencies() {
        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingMultipleSources::buildEntityDescriptor)
                .withMessageContaining(
                        "The entity class (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingMultipleSources)")
                .withMessageContaining("has an @CascadingUpdateShadowVariable annotated properties")
                .withMessageContaining("but the sources can be either a regular shadow variable or a planning list variable")
                .withMessageContaining(
                        "Maybe update sourceVariableName or sourceVariableNames and reference either regular shadow variables or a planning list variable")
                .withMessageContaining(
                        "Maybe configure a distinct targetMethodName for one of the source types, and a separate listener will be created for it");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingWrongEntity::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongEntity)")
                .withMessageContaining("has a @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining("with a sourceEntityClass (class java.lang.Object) which is not a valid planning entity")
                .withMessageContaining("Maybe check the annotations of the class (class java.lang.Object)")
                .withMessageContaining(
                        "Maybe add the class (class java.lang.Object) among planning entities in the solver configuration");

        assertThatIllegalArgumentException().isThrownBy(TestdataCascadingWrongSourceWithEntity::buildEntityDescriptor)
                .withMessageContaining(
                        "The entityClass (class ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.TestdataCascadingWrongSourceWithEntity)")
                .withMessageContaining("has a @CascadingUpdateShadowVariable annotated property (cascadeValue)")
                .withMessageContaining(
                        "with a sourceEntityClass (class ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingEntity)")
                .withMessageContaining(
                        "but the shadow variable \"bad\" cannot be found in the planning entity class ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingEntity")
                .withMessageContaining(
                        "Maybe update sourceVariableName to an existing shadow variable in the entity class ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingEntity");
    }

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingSouceSolution> variableDescriptor =
                TestdataSingleCascadingSourceEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingSouceSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingSouceSolution solution =
                TestdataSingleCascadingSouceSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataSingleCascadingSourceEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(1);
    }

    @Test
    void updateOnlyMiddleValue() {
        GenuineVariableDescriptor<TestdataSingleCascadingSouceSolution> variableDescriptor =
                TestdataSingleCascadingSourceEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingSouceSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingSouceSolution solution =
                TestdataSingleCascadingSouceSolution.generateUninitializedSolution(3, 2);
        TestdataSingleCascadingSourceEntity entity = solution.getEntityList().get(0);
        entity.setValueList(solution.getValueList());
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 1, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isZero();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from previous element change
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(4);
        // Called from update next val2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(1);
    }

    @Test
    void stopUpdateNextValues() {
        GenuineVariableDescriptor<TestdataSingleCascadingSouceSolution> variableDescriptor =
                TestdataSingleCascadingSourceEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataSingleCascadingSouceSolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataSingleCascadingSouceSolution solution =
                TestdataSingleCascadingSouceSolution.generateUninitializedSolution(3, 2);
        TestdataSingleCascadingSourceEntity entity = solution.getEntityList().get(0);
        entity.setValueList(solution.getValueList());
        solution.getValueList().get(1).setCascadeValue(3);
        scoreDirector.setWorkingSolution(solution);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 1);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        // Called from update next val1 and previous element change
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isNull();
        // Stop on value2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isZero();
    }
}
