package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

/**
 * Covers the merge mechanism in {@link VariableChangeRecordingScoreDirector}:
 * a {@code beforeListVariableChanged}/{@code afterListVariableChanged} pair for the same entity
 * is folded into a single {@link ListVariableBeforeChangeAction},
 * so its undo fires one {@code afterListVariableChanged} notification instead of two -
 * regardless of what sits between the two calls in recording order,
 * since real move implementations do not always call them back to back
 * (cross-entity swaps and k-opt batch several entities' befores, then several afters).
 */
class VariableChangeRecordingScoreDirectorTest {

    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();

    @SuppressWarnings("unchecked")
    private InnerScoreDirector<TestdataListSolution, ?> mockBacking() {
        return mock(InnerScoreDirector.class);
    }

    @Test
    void sameListSingleElementChange_mergesIntoOneUndoNotification() {
        var v0 = new TestdataListValue("0");
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var entity = new TestdataListEntity("e", v0, v1, v2);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        // Simulate a same-list change: v1 at index 1 is replaced by a new value, same range size.
        recorder.beforeListVariableChanged(variableDescriptor, entity, 1, 2);
        var vNew = new TestdataListValue("new");
        entity.getValueList().set(1, vNew);
        recorder.afterListVariableChanged(variableDescriptor, entity, 1, 2);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(entity.getValueList()).containsExactly(v0, v1, v2);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 1, 2);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 1, 2);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void crossListSingleElementMove_bothEntitiesMergeIndependently() {
        var v0 = new TestdataListValue("v0");
        var entityA = new TestdataListEntity("a", v0);
        var entityB = new TestdataListEntity("b");
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        // Remove v0 from A...
        recorder.beforeListVariableChanged(variableDescriptor, entityA, 0, 1);
        entityA.getValueList().removeFirst();
        recorder.afterListVariableChanged(variableDescriptor, entityA, 0, 0);
        // ...and insert it into B.
        recorder.beforeListVariableChanged(variableDescriptor, entityB, 0, 0);
        entityB.getValueList().addFirst(v0);
        recorder.afterListVariableChanged(variableDescriptor, entityB, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(entityA.getValueList()).containsExactly(v0);
        assertThat(entityB.getValueList()).isEmpty();
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entityA, 0, 0);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entityA, 0, 1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entityB, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entityB, 0, 0);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void crossEntitySwap_nonAdjacentBracketsStillMerge() {
        // [BeforeL, BeforeR, AfterL, AfterR] - the shape MoveDirector.swapValuesBetweenLists
        // actually records for a cross-entity swap. Confirms matching is by entity identity,
        // not list position.
        var vLeft = new TestdataListValue("left");
        var vRight = new TestdataListValue("right");
        var entityL = new TestdataListEntity("l", vLeft);
        var entityR = new TestdataListEntity("r", vRight);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        recorder.beforeListVariableChanged(variableDescriptor, entityL, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, entityR, 0, 1);
        entityL.getValueList().set(0, vRight);
        entityR.getValueList().set(0, vLeft);
        recorder.afterListVariableChanged(variableDescriptor, entityL, 0, 1);
        recorder.afterListVariableChanged(variableDescriptor, entityR, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(entityL.getValueList()).containsExactly(vLeft);
        assertThat(entityR.getValueList()).containsExactly(vRight);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entityL, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entityL, 0, 1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entityR, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entityR, 0, 1);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void lifoNestedBrackets_stillMergeIndependently() {
        // [BeforeDest, BeforeSrc, AfterSrc, AfterDest] - MoveDirector.replaceValue's shape.
        var vDest = new TestdataListValue("dest");
        var vSrc = new TestdataListValue("src");
        var dest = new TestdataListEntity("dest", vDest);
        var src = new TestdataListEntity("src", vSrc);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        recorder.beforeListVariableChanged(variableDescriptor, dest, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, src, 0, 1);
        src.getValueList().set(0, vDest);
        recorder.afterListVariableChanged(variableDescriptor, src, 0, 1);
        dest.getValueList().set(0, vSrc);
        recorder.afterListVariableChanged(variableDescriptor, dest, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(dest.getValueList()).containsExactly(vDest);
        assertThat(src.getValueList()).containsExactly(vSrc);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, dest, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, dest, 0, 1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, src, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, src, 0, 1);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void kOptStyleBatchedBrackets_threeEntitiesEachMergeIndependently() {
        // [BeforeE1, BeforeE2, BeforeE3, AfterE1, AfterE2, AfterE3] - k-opt's fully-batched shape.
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var e1 = new TestdataListEntity("e1", v1);
        var e2 = new TestdataListEntity("e2", v2);
        var e3 = new TestdataListEntity("e3", v3);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        recorder.beforeListVariableChanged(variableDescriptor, e1, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, e2, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, e3, 0, 1);
        e1.getValueList().set(0, v2);
        e2.getValueList().set(0, v3);
        e3.getValueList().set(0, v1);
        recorder.afterListVariableChanged(variableDescriptor, e1, 0, 1);
        recorder.afterListVariableChanged(variableDescriptor, e2, 0, 1);
        recorder.afterListVariableChanged(variableDescriptor, e3, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(e1.getValueList()).containsExactly(v1);
        assertThat(e2.getValueList()).containsExactly(v2);
        assertThat(e3.getValueList()).containsExactly(v3);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, e1, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, e1, 0, 1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, e2, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, e2, 0, 1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, e3, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, e3, 0, 1);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void sameEntityTwoIndependentBrackets_doNotCrossMerge() {
        // A mass-move-shaped case: entity E loses v1 at index 0 (bracket 1), then separately
        // gains v3 at the new index 0 (bracket 2) - MoveDirector.massMoveValues's shape when a
        // destination entity is also a source entity.
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var v3 = new TestdataListValue("3");
        var entity = new TestdataListEntity("e", v1, v2);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        // Bracket 1: remove v1.
        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        entity.getValueList().removeFirst();
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 0);
        // Bracket 2, independent and already closed: insert v3 at the front.
        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 0);
        entity.getValueList().addFirst(v3);
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        // Undo runs in reverse: bracket 2 first (removes v3), then bracket 1 (re-adds v1).
        assertThat(entity.getValueList()).containsExactly(v1, v2);
        // Two independent merged actions -> two notification pairs, not one and not a cross-merge.
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 0, 1); // Bracket 2's undo.
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 0); // Bracket 2's undo.
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 0, 0); // Bracket 1's undo.
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 1); // Bracket 1's undo.
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void requiresIndexCacheTrue_mismatchedFromIndexStillThrows() {
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, true);

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> recorder.afterListVariableChanged(variableDescriptor, entity, 5, 1));
    }

    @Test
    void requiresIndexCacheFalse_mergeStillWorksWithoutValidationCache() {
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        var vNew = new TestdataListValue("new");
        entity.getValueList().set(0, vNew);
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(entity.getValueList()).containsExactly(v0);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void getNonDelegating_sharesThePendingTrackerAcrossInstances() {
        // SelectorBasedListRuinRecreateMove's shape: before is recorded on one recording
        // instance, the matching after fires on getNonDelegating()'s separate instance.
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, true);
        var nonDelegating = recorder.getNonDelegating();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        var vNew = new TestdataListValue("new");
        entity.getValueList().set(0, vNew);
        nonDelegating.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(entity.getValueList()).containsExactly(v0);
        // One merged notification pair, not two separate actions - the cross-instance pair
        // merged, it did not silently fall back to two independent, unmerged actions.
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void orphanedAfterWithNoBefore_fallsBackToStandaloneUndo() {
        var v0 = new TestdataListValue("0");
        var v1 = new TestdataListValue("1");
        var entity = new TestdataListEntity("e", v0, v1);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        // No beforeListVariableChanged call at all for this entity.
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        // ListVariableAfterChangeAction.undo(): clears [0,1) and notifies (0,0).
        assertThat(entity.getValueList()).containsExactly(v1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 0);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void neverMergedBecauseNoAfterArrived_undoesExactlyLikeAnUnmergedBeforeAction() {
        // Simulates an exception thrown between before and after: the pending before-action sits
        // in the list unmerged when undoChanges() eventually runs.
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        // No matching afterListVariableChanged call - as if an exception aborted the move here.

        clearInvocations(backing);
        recorder.undoChanges();

        // Unmerged undo: re-add oldValue, single notification with the ORIGINAL (fromIndex, toIndex).
        assertThat(entity.getValueList()).containsExactly(v0, v0);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing).updateShadowVariables();
        verifyNoMoreInteractions(backing);
    }

    @Test
    void undoChangesClearsThePendingTracker_soALaterOrphanedAfterDoesNotMatchAStaleEntry() {
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing, false);

        // Move 1 aborts right after "before" fires; no matching "after" ever arrives - as if an
        // exception terminated the move (EphemeralMoveDirector never calls close()/undoChanges()
        // on that path in production; calling undoChanges() here directly stands in for whatever
        // eventually resets this recording session). What matters is whether the pending entry
        // survives that reset.
        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        recorder.undoChanges();

        // A later, unrelated bare "after" for the same entity (no "before" of its own - the rare
        // orphan case) must not find and merge with move 1's stale pending entry.
        entity.setValueList(new ArrayList<>(List.of(v0, v0)));
        clearInvocations(backing);
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);
        recorder.undoChanges();

        // ListVariableAfterChangeAction's standalone undo fired - proving a fresh, standalone
        // action was appended, not a silent (and destructive) merge into the discarded stale one.
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entity, 0, 0);
    }

}
