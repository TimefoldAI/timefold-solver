package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.Lookup;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class ListVariableBeforeChangeActionTest {

    private final VariableDescriptorAwareScoreDirector<TestdataListSolution> scoreDirector =
            mock(VariableDescriptorAwareScoreDirector.class);
    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();

    @Test
    void undoWithEmptyOldValueIsANoOp() {
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        // fromIndex == toIndex == 1: a pure insert recorded nothing to restore.
        var action = new ListVariableBeforeChangeAction<>(entity, List.of(), 1, 1, variableDescriptor);

        action.undo(scoreDirector);

        assertThat(entity.getValueList()).containsExactly(v0);
        // The sibling ListVariableAfterChangeAction's undo already notified for this range;
        // this action must not fire a redundant duplicate.
        verifyNoInteractions(scoreDirector);
    }

    @Test
    void undoWithNonEmptyOldValueRestoresAndNotifies() {
        var v0 = new TestdataListValue("0");
        var v1 = new TestdataListValue("1");
        var entity = new TestdataListEntity("e", v1);
        var action = new ListVariableBeforeChangeAction<>(entity, List.of(v0), 0, 1, variableDescriptor);

        action.undo(scoreDirector);

        assertThat(entity.getValueList()).extracting(TestdataListValue::toString).containsExactly("0", "1");
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, entity, 0, 1);
    }

    @Test
    void rebasePreservesMergedState() {
        var v0 = new TestdataListValue("0");
        var vNew = new TestdataListValue("new");
        var originalEntity = new TestdataListEntity("e", vNew);
        var action = new ListVariableBeforeChangeAction<>(originalEntity, List.of(v0), 0, 1, variableDescriptor);
        action.merge(1); // Simulate the matching after() call that would fire during recording.

        // Rebase to genuinely different instances - not identity - so this test cannot pass merely
        // because rebase() happened to leave everything pointing at the original objects.
        var rebasedV0 = new TestdataListValue("0");
        var rebasedVNew = new TestdataListValue("new");
        var rebasedEntity = new TestdataListEntity("e", rebasedVNew);
        var lookup = mock(Lookup.class);
        when(lookup.lookUpWorkingObject(originalEntity)).thenReturn(rebasedEntity);
        when(lookup.lookUpWorkingObject(v0)).thenReturn(rebasedV0);

        var rebasedAction = action.rebase(lookup);
        var rebasedScoreDirector = mock(VariableDescriptorAwareScoreDirector.class);
        rebasedAction.undo(rebasedScoreDirector);

        assertThat(rebasedEntity.getValueList()).hasSize(1);
        assertThat(rebasedEntity.getValueList().getFirst()).isSameAs(rebasedV0);
        // Both calls firing - not just afterListVariableChanged - proves the merged undo path ran.
        // Before the fix, rebase() dropped the merged state, so the rebased copy fell through to
        // the unmerged fallback (no beforeListVariableChanged call, and it would have blindly
        // addAll'd oldValue without clearing rebasedVNew first, corrupting the list).
        verify(rebasedScoreDirector).beforeListVariableChanged(variableDescriptor, rebasedEntity, 0, 1);
        verify(rebasedScoreDirector).afterListVariableChanged(variableDescriptor, rebasedEntity, 0, 1);
    }

}
