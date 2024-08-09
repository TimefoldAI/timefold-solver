package ai.timefold.solver.core.impl.domain.variable.cascade;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.testdata.domain.cascade.different_var.TestdataDifferentCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.different_var.TestdataDifferentCascadingSolution;
import ai.timefold.solver.core.impl.testdata.util.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class DifferentCascadingUpdateShadowVariableListenerTest {

    @Test
    void updateAllNextValues() {
        var variableDescriptor = TestdataDifferentCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataDifferentCascadingSolution.generateUninitializedSolution(3, 2);
        scoreDirector.setWorkingSolution(solution);

        var entity = solution.getEntityList().get(0);
        scoreDirector.beforeListVariableChanged(entity, "valueList", 0, 0);
        entity.setValueList(solution.getValueList());
        scoreDirector.afterListVariableChanged(entity, "valueList", 0, 3);
        scoreDirector.triggerVariableListeners();

        assertThat(entity.getValueList().get(0).getCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();
        assertThat(entity.getValueList().get(0).getSecondNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
        // Called from update next val1, inverse and previous element changes
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isOne();
        assertThat(entity.getValueList().get(1).getSecondNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
        // Called from update next val2, inverse and previous element changes
        assertThat(entity.getValueList().get(2).getSecondNumberOfCalls()).isOne();
    }

    @Test
    void stopUpdateNextValues() {
        var variableDescriptor = TestdataDifferentCascadingEntity.buildVariableDescriptorForValueList();

        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(variableDescriptor.getEntityDescriptor().getSolutionDescriptor());

        var solution = TestdataDifferentCascadingSolution.generateUninitializedSolution(3, 2);
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
        assertThat(entity.getValueList().get(0).getSecondCascadeValue()).isEqualTo(2);
        assertThat(entity.getValueList().get(0).getNumberOfCalls()).isOne();
        assertThat(entity.getValueList().get(0).getSecondNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(1).getCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getSecondCascadeValue()).isEqualTo(3);
        assertThat(entity.getValueList().get(1).getNumberOfCalls()).isOne();
        assertThat(entity.getValueList().get(1).getSecondNumberOfCalls()).isOne();

        assertThat(entity.getValueList().get(2).getCascadeValue()).isNull();
        assertThat(entity.getValueList().get(2).getSecondCascadeValue()).isEqualTo(4);
        // Stop only on the value2 from the cascadeValue
        assertThat(entity.getValueList().get(2).getNumberOfCalls()).isZero();
        assertThat(entity.getValueList().get(2).getSecondNumberOfCalls()).isOne();
    }
}
