package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    void undoRestoresTheCapturedRangeAndNotifiesOnce() {
        var v0 = new TestdataListValue("0");
        var vNew = new TestdataListValue("new");
        var entity = new TestdataListEntity("e", vNew);
        var action = new ListVariableBeforeChangeAction<>(entity, List.of(v0), 0, 1, 1, variableDescriptor);
        action.updateToIndex(1); // The matching after() call that would fire during recording.

        action.undo(scoreDirector);

        assertThat(entity.getValueList()).containsExactly(v0);
        // One pair: before() over the mutated range, after() over the restored range.
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, entity, 0, 1);
    }

    @Test
    void undoOfAPureInsertClearsWhatWasInserted() {
        // fromIndex == toIndex: the bracket captured nothing, because the mutation only added.
        var inserted = new TestdataListValue("inserted");
        var entity = new TestdataListEntity("e", inserted);
        var action = new ListVariableBeforeChangeAction<>(entity, List.<TestdataListValue> of(), 0, 0, 0,
                variableDescriptor);
        action.updateToIndex(1);

        action.undo(scoreDirector);

        assertThat(entity.getValueList()).isEmpty();
        verify(scoreDirector).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(scoreDirector).afterListVariableChanged(variableDescriptor, entity, 0, 0);
    }

    @Test
    void undoOfAnUnmergedActionFailsFast() {
        // Never merged: nothing recorded how far the mutation reached, so undo cannot clear it.
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", new TestdataListValue("new"));
        var action = new ListVariableBeforeChangeAction<>(entity, List.of(v0), 0, 1, 1, variableDescriptor);

        assertThatIllegalStateException()
                .isThrownBy(() -> action.undo(scoreDirector))
                .withMessageContaining("was never closed by its afterListVariableChanged");
    }

    @Test
    void rebasePreservesMergedState() {
        var v0 = new TestdataListValue("0");
        var vNew = new TestdataListValue("new");
        var originalEntity = new TestdataListEntity("e", vNew);
        var action = new ListVariableBeforeChangeAction<>(originalEntity, List.of(v0), 0, 1, 1, variableDescriptor);
        action.updateToIndex(1); // Simulate the matching after() call that would fire during recording.

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
        // If rebase() dropped the merged state, the rebased copy would instead fail fast.
        verify(rebasedScoreDirector).beforeListVariableChanged(variableDescriptor, rebasedEntity, 0, 1);
        verify(rebasedScoreDirector).afterListVariableChanged(variableDescriptor, rebasedEntity, 0, 1);
    }

}
