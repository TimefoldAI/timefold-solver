package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListVariableAfterChangeActionTest {

    private final ListVariableAfterChangeAction<TestdataListSolution, TestdataListEntity, TestdataListValue> action;
    private final VariableDescriptorAwareScoreDirector<TestdataListSolution> scoreDirector =
            mock(VariableDescriptorAwareScoreDirector.class);
    private final TestdataListEntity entity;
    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();

    ListVariableAfterChangeActionTest() {
        var v0 = new TestdataListValue("0");
        var v1 = new TestdataListValue("1");
        entity = new TestdataListEntity("e", v0, v1);
        // Undoing an after-change action of [0, 1) means the values that were inserted/changed there
        // (v0) must be removed again, shrinking the list back down.
        action = new ListVariableAfterChangeAction<>(entity, 0, 1, variableDescriptor);
    }

    @Test
    void undoClearsTheRange() {
        action.undo(scoreDirector);

        assertThat(entity.getValueList()).extracting(TestdataListValue::toString).containsExactly("1");
    }

    @Test
    void undoNotifiesAfterListVariableChangedOnceTheRangeIsCleared() {
        action.undo(scoreDirector);

        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        // The range collapsed to empty once cleared; downstream shadow variable tracking
        // (index, inverse relation, ...) must be told about it, or its state goes stale.
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, entity, 0, 0);
        verifyNoMoreInteractions(scoreDirector);
    }

}
