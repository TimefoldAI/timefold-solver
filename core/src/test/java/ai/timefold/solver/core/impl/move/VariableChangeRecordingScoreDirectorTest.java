package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
 * <p>
 * The merge only works on a well-formed bracket,
 * so the recorder validates every event against the bracket it claims to belong to.
 * The {@code failsFast} tests below cover those rules;
 * each of them silently corrupted the list variable
 * (or threw {@link IndexOutOfBoundsException} from deep inside undo)
 * before the rules existed.
 */
class VariableChangeRecordingScoreDirectorTest {

    private final ListVariableDescriptor<TestdataListSolution> variableDescriptor =
            TestdataListEntity.buildVariableDescriptorForValueList();

    @SuppressWarnings("unchecked")
    private static InnerScoreDirector<TestdataListSolution, SimpleScore> mockBacking() {
        return mock(InnerScoreDirector.class);
    }

    private static VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore> recorder() {
        return new VariableChangeRecordingScoreDirector<>(mockBacking());
    }

    @Test
    void sameListSingleElementChange_mergesIntoOneUndoNotification() {
        var v0 = new TestdataListValue("0");
        var v1 = new TestdataListValue("1");
        var v2 = new TestdataListValue("2");
        var entity = new TestdataListEntity("e", v0, v1, v2);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

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
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

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
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

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
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

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
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

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
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

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
    void sameEntitySequentialBracketsWithIdenticalIndexes_undoRestoresOriginal() {
        // Two operations in one move whose brackets carry the exact same (fromIndex, toIndex).
        // Nothing may cross-merge them: each has its own oldValue to restore.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var entity = new TestdataListEntity("e", v0, v1);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        entity.getValueList().set(0, new TestdataListValue("x"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        entity.getValueList().set(0, new TestdataListValue("y"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertThat(entity.getValueList()).containsExactly(v0, v1);
        verify(backing, times(2)).beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        verify(backing, times(2)).afterListVariableChanged(variableDescriptor, entity, 0, 1);
    }

    @Test
    void sameEntitySequentialOverlappingBrackets_undoRestoresOriginal() {
        // The second bracket strictly contains the first one's range and shrinks it.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var entity = new TestdataListEntity("e", v0, v1, v2, v3);
        var recorder = recorder();
        var list = entity.getValueList();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 1, 2);
        list.set(1, new TestdataListValue("x"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 1, 2);
        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 3);
        list.subList(0, 3).clear();
        list.addAll(0, List.of(new TestdataListValue("p"), new TestdataListValue("q")));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 2);

        recorder.undoChanges();

        assertThat(entity.getValueList()).containsExactly(v0, v1, v2, v3);
    }

    @Test
    void sameEntitySequentialBracketsTouchingAtABoundary_undoRestoresOriginal() {
        // Bracket 1 collapses [0, 2) to [0, 1); bracket 2 then starts at index 1 - the very index
        // that shrink created. Undo has to unwind them in the right order or they overlap.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var entity = new TestdataListEntity("e", v0, v1, v2, v3);
        var recorder = recorder();
        var list = entity.getValueList();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
        list.subList(0, 2).clear();
        list.addFirst(new TestdataListValue("x"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, entity, 1, 3);
        list.subList(1, 3).clear();
        list.add(1, new TestdataListValue("y"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 1, 2);

        recorder.undoChanges();

        assertThat(entity.getValueList()).containsExactly(v0, v1, v2, v3);
    }

    @Test
    void getNonDelegating_sharesThePendingTrackerAcrossInstances() {
        // SelectorBasedListRuinRecreateMove's shape: before is recorded on one recording
        // instance, the matching after fires on getNonDelegating()'s separate instance.
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);
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
    void getNonDelegating_twoEntitiesPendingAcrossInstances_bothStillMerge() {
        // Ruin-recreate ruins several entities in one move: their befores queue up on the delegating
        // instance (escalating the shared tracker to its overflow map) and only close, one by one,
        // on the non-delegating copy after the nested phase has rebuilt the lists.
        var vLeft = new TestdataListValue("left");
        var vRight = new TestdataListValue("right");
        var entityL = new TestdataListEntity("l", vLeft);
        var entityR = new TestdataListEntity("r", vRight);
        var backing = mockBacking();
        var recorder = new VariableChangeRecordingScoreDirector<TestdataListSolution, SimpleScore>(backing);
        var nonDelegating = recorder.getNonDelegating();

        recorder.beforeListVariableChanged(variableDescriptor, entityL, 0, 1);
        recorder.beforeListVariableChanged(variableDescriptor, entityR, 0, 1);
        entityL.getValueList().set(0, vRight);
        entityR.getValueList().set(0, vLeft);
        nonDelegating.afterListVariableChanged(variableDescriptor, entityL, 0, 1);
        nonDelegating.afterListVariableChanged(variableDescriptor, entityR, 0, 1);

        clearInvocations(backing);
        recorder.undoChanges();

        assertSoftly(softly -> {
            softly.assertThat(entityL.getValueList()).containsExactly(vLeft);
            softly.assertThat(entityR.getValueList()).containsExactly(vRight);
        });
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entityL, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entityL, 0, 1);
        verify(backing, times(1)).beforeListVariableChanged(variableDescriptor, entityR, 0, 1);
        verify(backing, times(1)).afterListVariableChanged(variableDescriptor, entityR, 0, 1);
    }

    @Test
    void createUndoMoveWhileABracketIsStillOpen_isRetroactivelyMerged() {
        // createUndoMove() hands out the live change list, not a snapshot: the action inside the
        // returned move is unmerged when it escapes and merged by the time it is replayed. This pins
        // that aliasing down, since the split-recorder path (getNonDelegating()) relies on it.
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var recorder = recorder();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        var undoMove = recorder.createUndoMove();
        entity.getValueList().set(0, new TestdataListValue("x"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        undoMove.execute(new MoveDirector<TestdataListSolution, SimpleScore>(mockBacking()));

        assertThat(entity.getValueList()).containsExactly(v0);
    }

    @Test
    void undoChangesReleasesThePendingTracker_soTheSameEntityCanOpenAFreshBracket() {
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var recorder = recorder();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        entity.getValueList().set(0, new TestdataListValue("x"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);
        recorder.undoChanges();

        // A stale pending entry would make the next move's bracket for the same entity fail.
        assertThatCode(() -> recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1))
                .doesNotThrowAnyException();
    }

    @Test
    void secondBracketForAnOpenEntity_failsFast() {
        // Two concurrently open brackets for one entity. Whichever after arrives first, the pairing
        // is guesswork: previously the inner before evicted the outer one, and undo then either
        // threw IndexOutOfBoundsException or silently grew the list from 4 elements to 7.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var v3 = new TestdataListValue("v3");
        var entity = new TestdataListEntity("e", v0, v1, v2, v3);
        var recorder = recorder();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 4);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> recorder.beforeListVariableChanged(variableDescriptor, entity, 1, 3))
                .withMessageContaining("already has an open beforeListVariableChanged");
    }

    @Test
    void orphanedAfterWithNoBefore_failsFast() {
        var v0 = new TestdataListValue("0");
        var v1 = new TestdataListValue("1");
        var entity = new TestdataListEntity("e", v0, v1);
        var recorder = recorder();

        // No beforeListVariableChanged call at all for this entity.
        assertThatIllegalArgumentException()
                .isThrownBy(() -> recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1))
                .withMessageContaining("has no matching beforeListVariableChanged");
    }

    @Test
    void duplicateAfterForAClosedBracket_failsFast() {
        // A move (or a listener chain) notifying the same closed bracket twice. The second after used
        // to be recorded as a standalone action, so undo cleared the range twice but restored it once,
        // silently deleting an element.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var entity = new TestdataListEntity("e", v0, v1);
        var recorder = recorder();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);
        entity.getValueList().set(0, new TestdataListValue("x"));
        recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> recorder.afterListVariableChanged(variableDescriptor, entity, 0, 1))
                .withMessageContaining("has no matching beforeListVariableChanged");
    }

    @Test
    void mismatchedAfterFromIndexThrows() {
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var recorder = recorder();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> recorder.afterListVariableChanged(variableDescriptor, entity, 5, 1))
                .withMessageContaining("must match the fromIndex of its beforeListVariableChanged counterpart");
    }

    @Test
    void getNonDelegating_driftedFromIndexAcrossInstances_failsFast() {
        // Ruin-recreate derives both ends of the bracket from getFirstUnpinnedIndex(entity),
        // recomputed after the nested phase has run. If the pinned prefix moved in between, the after
        // call's fromIndex no longer matches the before call's - and the two calls land on different
        // recorder instances, so the check has to survive the split.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("e", v0, v1, v2);
        var recorder = recorder();
        var nonDelegating = recorder.getNonDelegating();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 1, 3);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> nonDelegating.afterListVariableChanged(variableDescriptor, entity, 2, 3))
                .withMessageContaining("must match the fromIndex of its beforeListVariableChanged counterpart");
    }

    @Test
    void afterUnderReportingTheLengthChange_failsFast() {
        // The bracket declared [0, 2) and the mutation replaced those two elements with three, but the
        // after call still reports [0, 2). Undo trusts that range: it clears [0, 2) and restores two
        // elements, silently deleting v2. Nothing else in the event stream reveals this, so the
        // recorder compares the list's actual length change against the reported one.
        var v0 = new TestdataListValue("v0");
        var v1 = new TestdataListValue("v1");
        var v2 = new TestdataListValue("v2");
        var entity = new TestdataListEntity("e", v0, v1, v2);
        var recorder = recorder();
        var list = entity.getValueList();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 2);
        list.subList(0, 2).clear();
        list.addAll(0, List.of(new TestdataListValue("x"), new TestdataListValue("y"), new TestdataListValue("z")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> recorder.afterListVariableChanged(variableDescriptor, entity, 0, 2))
                .withMessageContaining("actually changed length by");
    }

    @Test
    void unclosedBracketAtUndoTime_failsFast() {
        // A bracket left open - as if an exception aborted the move between before and after. The
        // action cannot be undone: nothing recorded how far the mutation reached.
        var v0 = new TestdataListValue("0");
        var entity = new TestdataListEntity("e", v0);
        var recorder = recorder();

        recorder.beforeListVariableChanged(variableDescriptor, entity, 0, 1);

        assertThatIllegalStateException()
                .isThrownBy(recorder::undoChanges)
                .withMessageContaining("was never closed by its afterListVariableChanged");
    }

}
