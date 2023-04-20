package ai.timefold.solver.core.impl.heuristic.selector.value.mimic;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCode;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;

import org.junit.jupiter.api.Test;

class MimicReplayingValueSelectorTest {

    @Test
    void originalSelection() {
        EntityIndependentValueSelector childValueSelector = SelectorTestUtils.mockEntityIndependentValueSelector(
                TestdataEntity.class, "value",
                new TestdataValue("v1"), new TestdataValue("v2"), new TestdataValue("v3"));

        MimicRecordingValueSelector recordingValueSelector = new MimicRecordingValueSelector(childValueSelector);
        MimicReplayingValueSelector replayingValueSelector = new MimicReplayingValueSelector(recordingValueSelector);

        SolverScope solverScope = mock(SolverScope.class);
        recordingValueSelector.solvingStarted(solverScope);
        replayingValueSelector.solvingStarted(solverScope);

        AbstractPhaseScope phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        recordingValueSelector.phaseStarted(phaseScopeA);
        replayingValueSelector.phaseStarted(phaseScopeA);

        AbstractStepScope stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        recordingValueSelector.stepStarted(stepScopeA1);
        replayingValueSelector.stepStarted(stepScopeA1);
        runOriginalAsserts(recordingValueSelector, replayingValueSelector);
        recordingValueSelector.stepEnded(stepScopeA1);
        replayingValueSelector.stepEnded(stepScopeA1);

        AbstractStepScope stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        recordingValueSelector.stepStarted(stepScopeA2);
        replayingValueSelector.stepStarted(stepScopeA2);
        runOriginalAsserts(recordingValueSelector, replayingValueSelector);
        recordingValueSelector.stepEnded(stepScopeA2);
        replayingValueSelector.stepEnded(stepScopeA2);

        recordingValueSelector.phaseEnded(phaseScopeA);
        replayingValueSelector.phaseEnded(phaseScopeA);

        AbstractPhaseScope phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        recordingValueSelector.phaseStarted(phaseScopeB);
        replayingValueSelector.phaseStarted(phaseScopeB);

        AbstractStepScope stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        recordingValueSelector.stepStarted(stepScopeB1);
        replayingValueSelector.stepStarted(stepScopeB1);
        runOriginalAsserts(recordingValueSelector, replayingValueSelector);
        recordingValueSelector.stepEnded(stepScopeB1);
        replayingValueSelector.stepEnded(stepScopeB1);

        recordingValueSelector.phaseEnded(phaseScopeB);
        replayingValueSelector.phaseEnded(phaseScopeB);

        recordingValueSelector.solvingEnded(solverScope);
        replayingValueSelector.solvingEnded(solverScope);

        verifyPhaseLifecycle(childValueSelector, 1, 2, 3);
        verify(childValueSelector, times(3)).iterator();
    }

    private void runOriginalAsserts(MimicRecordingValueSelector recordingValueSelector,
            MimicReplayingValueSelector replayingValueSelector) {
        Iterator<Object> recordingIterator = recordingValueSelector.iterator();
        assertThat(recordingIterator).isNotNull();
        Iterator<Object> replayingIterator = replayingValueSelector.iterator();
        assertThat(replayingIterator).isNotNull();

        assertThat(recordingIterator).hasNext();
        assertThat(replayingIterator).hasNext();
        assertCode("v1", recordingIterator.next());
        assertCode("v1", replayingIterator.next());
        assertThat(recordingIterator).hasNext();
        assertThat(replayingIterator).hasNext();
        assertCode("v2", recordingIterator.next());
        assertCode("v2", replayingIterator.next());
        // Extra call
        assertThat(replayingIterator).isExhausted();
        assertThat(recordingIterator).hasNext();
        assertThat(replayingIterator).hasNext();
        // Duplicated call
        assertThat(replayingIterator).hasNext();
        assertCode("v3", recordingIterator.next());
        assertCode("v3", replayingIterator.next());
        assertThat(recordingIterator).isExhausted();
        assertThat(replayingIterator).isExhausted();
        // Duplicated call
        assertThat(replayingIterator).isExhausted();

        assertThat(recordingValueSelector.isCountable()).isTrue();
        assertThat(replayingValueSelector.isCountable()).isTrue();
        assertThat(recordingValueSelector.isNeverEnding()).isFalse();
        assertThat(replayingValueSelector.isNeverEnding()).isFalse();
        assertThat(recordingValueSelector.getSize()).isEqualTo(3L);
        assertThat(replayingValueSelector.getSize()).isEqualTo(3L);
    }

}
