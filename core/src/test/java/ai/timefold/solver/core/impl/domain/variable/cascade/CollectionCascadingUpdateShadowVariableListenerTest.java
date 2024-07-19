package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.supply.TestdataMultipleCascadingWithSupplyEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.supply.TestdataMultipleCascadingWithSupplySolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.supply.TestdataMultipleCascadingWithSupplyValue;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class CollectionCascadingUpdateShadowVariableListenerTest {

    @Test
    void updateAllNextValues() {
        GenuineVariableDescriptor<TestdataMultipleCascadingWithSupplySolution> variableDescriptor =
                TestdataMultipleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataMultipleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataMultipleCascadingWithSupplySolution solution =
                TestdataMultipleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataMultipleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
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
    void updateOnlyMiddleValue() {
        GenuineVariableDescriptor<TestdataMultipleCascadingWithSupplySolution> variableDescriptor =
                TestdataMultipleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataMultipleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        { // Changing the first shadow var
            TestdataMultipleCascadingWithSupplySolution solution =
                    TestdataMultipleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
            solution.getValueList().get(2).setSecondCascadeValue(4);
            scoreDirector.setWorkingSolution(solution);

            TestdataMultipleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
            entity.setValueList(solution.getValueList().subList(0, 2));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
            scoreDirector.triggerVariableListeners();
            solution.getValueList().forEach(TestdataMultipleCascadingWithSupplyValue::reset);

            scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
            entity.setValueList(
                    List.of(solution.getValueList().get(0), solution.getValueList().get(2), solution.getValueList().get(1)));
            scoreDirector.afterListVariableChanged(entity, "valueList", 1, 2);
            scoreDirector.triggerVariableListeners();

            assertThat(entity.getValueList().get(0).getNumberOfCalls()).isZero();

            assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
            assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(4);
            // Called from previous and inverse element change
            assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(2);

            assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(2);
            assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(3);
            // Called from update next val
            assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(2);
        }

        { // Changing the second shadow var
            TestdataMultipleCascadingWithSupplySolution solution =
                    TestdataMultipleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
            solution.getValueList().get(2).setCascadeValue(3);
            scoreDirector.setWorkingSolution(solution);

            TestdataMultipleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
            scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
            solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
            entity.setValueList(solution.getValueList().subList(0, 2));
            scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
            scoreDirector.triggerVariableListeners();
            solution.getValueList().forEach(TestdataMultipleCascadingWithSupplyValue::reset);

            scoreDirector.beforeListVariableChanged(entity, "valueList", 1, 1);
            entity.setValueList(
                    List.of(solution.getValueList().get(0), solution.getValueList().get(2), solution.getValueList().get(1)));
            scoreDirector.afterListVariableChanged(entity, "valueList", 1, 2);
            scoreDirector.triggerVariableListeners();

            assertThat(entity.getValueList().get(0).getNumberOfCalls()).isZero();

            assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
            assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(4);
            // Called from previous and inverse element change
            assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(2);

            assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(2);
            assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(3);
            // Called from update next val
            assertThat(entity.getValueList().get(2).getNumberOfCalls()).isEqualTo(2);
        }
    }

    @Test
    void stopUpdateNextValues() {
        GenuineVariableDescriptor<TestdataMultipleCascadingWithSupplySolution> variableDescriptor =
                TestdataMultipleCascadingWithSupplyEntity.buildVariableDescriptorForValueList();

        InnerScoreDirector<TestdataMultipleCascadingWithSupplySolution, SimpleScore> scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        TestdataMultipleCascadingWithSupplySolution solution =
                TestdataMultipleCascadingWithSupplySolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        TestdataMultipleCascadingWithSupplyEntity entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList().subList(0, 2));
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
        scoreDirector.triggerVariableListeners();
        solution.getValueList().forEach(TestdataMultipleCascadingWithSupplyValue::reset);

        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(
                List.of(solution.getValueList().get(2), solution.getValueList().get(0), solution.getValueList().get(1)));
        solution.getValueList().get(2).setCascadeValue(3);
        solution.getValueList().get(2).setSecondCascadeValue(4);
        solution.getValueList().get(0).setCascadeValue(1);
        solution.getValueList().get(0).setSecondCascadeValue(2);
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 1);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(1);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(2);
        // Called from update next val1 and previous element change
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(1);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(3);
        // Stop on value2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isZero();
    }
}
