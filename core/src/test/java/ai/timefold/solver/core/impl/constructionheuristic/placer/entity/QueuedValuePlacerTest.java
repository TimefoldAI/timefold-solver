package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.impl.constructionheuristic.placer.entity.PlacementAssertions.assertValuePlacement;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedValuePlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicRecordingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicReplayingValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

class QueuedValuePlacerTest {

    @Test
    void oneMoveSelector() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        EntitySelector<TestdataSolution> entitySelector =
                SelectorTestUtils.mockEntitySelector(variableDescriptor.getEntityDescriptor(),
                        new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"));
        EntityIndependentValueSelector<TestdataSolution> valueSelector = SelectorTestUtils.mockEntityIndependentValueSelector(
                variableDescriptor,
                new TestdataValue("1"), new TestdataValue("2"));
        MimicRecordingValueSelector<TestdataSolution> recordingValueSelector =
                new MimicRecordingValueSelector<>(valueSelector);

        var moveSelector = new ChangeMoveSelector<>(entitySelector,
                new MimicReplayingValueSelector<>(recordingValueSelector), false);
        var placer = new QueuedValuePlacer<>(null, null, recordingValueSelector, moveSelector);

        var solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        Iterator<Placement<TestdataSolution>> placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertValuePlacement(placementIterator.next(), "1", "a", "b", "c");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertValuePlacement(placementIterator.next(), "2", "a", "b", "c");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).hasNext();
        var stepScopeA3 = mock(AbstractStepScope.class);
        when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA3);
        assertValuePlacement(placementIterator.next(), "1", "a", "b", "c");
        placer.stepEnded(stepScopeA3);

        // Requires adding ReinitializeVariableValueSelector complexity to work
        // assertFalse(placementIterator.hasNext());
        placer.phaseEnded(phaseScopeA);

        var phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeB);
        placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        placer.stepStarted(stepScopeB1);
        assertValuePlacement(placementIterator.next(), "1", "a", "b", "c");
        placer.stepEnded(stepScopeB1);

        placer.phaseEnded(phaseScopeB);

        placer.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 2, 4);
        verifyPhaseLifecycle(valueSelector, 1, 2, 4);
    }

    @Test
    void copy() {
        var variableDescriptor = TestdataEntity.buildVariableDescriptorForValue();
        EntitySelector<TestdataSolution> entitySelector =
                SelectorTestUtils.mockEntitySelector(variableDescriptor.getEntityDescriptor(),
                        new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"));
        EntityIndependentValueSelector<TestdataSolution> valueSelector = SelectorTestUtils.mockEntityIndependentValueSelector(
                variableDescriptor,
                new TestdataValue("1"), new TestdataValue("2"));
        MimicRecordingValueSelector<TestdataSolution> recordingValueSelector =
                new MimicRecordingValueSelector<>(valueSelector);

        var moveSelector = new ChangeMoveSelector<>(entitySelector,
                new MimicReplayingValueSelector<>(recordingValueSelector), false);
        var factory = mock(EntityPlacerFactory.class);
        var configPolicy = mock(HeuristicConfigPolicy.class);
        assertThatThrownBy(() -> new QueuedValuePlacer<>(null, null, recordingValueSelector, moveSelector).copy())
                .hasMessage("The entity placer cannot be copied.");
        var placer = new QueuedValuePlacer<TestdataSolution>(factory, configPolicy, recordingValueSelector, moveSelector);
        placer.copy();
        verify(factory, times(1)).buildEntityPlacer(any());
    }

}
