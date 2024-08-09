package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataMultipleCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataMultipleCascadingSolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.piggyback.TestdataPiggybackCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.piggyback.TestdataPiggybackCascadingSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class MultipleCascadingUpdateShadowVariableListenerTest {

    @Test
    void updateAllNextValues() {
        var variableDescriptor = TestdataMultipleCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataMultipleCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isOne();
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isOne();
    }

    @Test
    void updateAllNextPiggybackValues() {
        var variableDescriptor = TestdataPiggybackCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataPiggybackCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isOne();
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isOne();
    }

    @Test
    void stopUpdateNextValues() {
        var variableDescriptor = TestdataMultipleCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataMultipleCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList().subList(0, 2));
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
        scoreDirector.triggerVariableListeners();

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
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isOne();
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(2);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(3);
        // Stop on value2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isOne();
    }

    @Test
    void stopUpdateNextPiggybackValues() {
        var variableDescriptor = TestdataPiggybackCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataPiggybackCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        solution.getValueList().subList(0, 2).forEach(v -> v.setEntity(entity));
        entity.setValueList(solution.getValueList().subList(0, 2));
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 2);
        scoreDirector.triggerVariableListeners();

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
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isOne();
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isEqualTo(2);

        assertThat(entity.getValueList().get(2).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(3);
        // Stop on value2
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isOne();
    }
}
