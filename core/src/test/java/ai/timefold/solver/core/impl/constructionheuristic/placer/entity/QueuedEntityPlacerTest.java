package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.impl.constructionheuristic.placer.entity.PlacementAssertions.assertEntityPlacement;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.testutil.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacerFactory;
import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicRecordingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicReplayingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.composite.CartesianProductMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.testdomain.multivar.TestdataMultiVarSolution;

import org.junit.jupiter.api.Test;

class QueuedEntityPlacerTest {

    @Test
    void oneMoveSelector() {
        EntitySelector<TestdataSolution> entitySelector = SelectorTestUtils.mockEntitySelector(TestdataEntity.class,
                new TestdataEntity("a"), new TestdataEntity("b"), new TestdataEntity("c"));
        MimicRecordingEntitySelector<TestdataSolution> recordingEntitySelector =
                new MimicRecordingEntitySelector<>(entitySelector);
        ValueSelector<TestdataSolution> valueSelector = SelectorTestUtils.mockValueSelector(TestdataEntity.class, "value",
                new TestdataValue("1"), new TestdataValue("2"));

        var moveSelector =
                new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector), valueSelector, false);
        var placer = new QueuedEntityPlacer<>(null, null, recordingEntitySelector, Collections.singletonList(moveSelector));

        var solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        var placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertEntityPlacement(placementIterator.next(), "a", "1", "2");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertEntityPlacement(placementIterator.next(), "b", "1", "2");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).hasNext();
        var stepScopeA3 = mock(AbstractStepScope.class);
        when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA3);
        assertEntityPlacement(placementIterator.next(), "c", "1", "2");
        placer.stepEnded(stepScopeA3);

        assertThat(placementIterator).isExhausted();
        placer.phaseEnded(phaseScopeA);

        var phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeB);
        placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        placer.stepStarted(stepScopeB1);
        assertEntityPlacement(placementIterator.next(), "a", "1", "2");
        placer.stepEnded(stepScopeB1);

        placer.phaseEnded(phaseScopeB);

        placer.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 2, 4);
        verifyPhaseLifecycle(valueSelector, 1, 2, 4);
    }

    @Test
    void multiQueuedMoveSelector() {
        EntitySelector<TestdataMultiVarSolution> entitySelector =
                SelectorTestUtils.mockEntitySelector(TestdataMultiVarEntity.class,
                        new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b"));
        MimicRecordingEntitySelector<TestdataMultiVarSolution> recordingEntitySelector =
                new MimicRecordingEntitySelector<>(entitySelector);
        ValueSelector<TestdataMultiVarSolution> primaryValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataMultiVarEntity.class, "primaryValue",
                new TestdataValue("1"), new TestdataValue("2"), new TestdataValue("3"));
        ValueSelector<TestdataMultiVarSolution> secondaryValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataMultiVarEntity.class, "secondaryValue",
                new TestdataValue("8"), new TestdataValue("9"));

        var moveSelectorList = new ArrayList<MoveSelector<TestdataMultiVarSolution>>(2);
        moveSelectorList.add(new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector),
                primaryValueSelector,
                false));
        moveSelectorList.add(new ChangeMoveSelector<>(
                new MimicReplayingEntitySelector<>(recordingEntitySelector),
                secondaryValueSelector,
                false));
        var placer =
                new QueuedEntityPlacer<>(null, null, recordingEntitySelector, moveSelectorList);

        var solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        Iterator<Placement<TestdataMultiVarSolution>> placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertEntityPlacement(placementIterator.next(), "a", "1", "2", "3");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertEntityPlacement(placementIterator.next(), "a", "8", "9");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).hasNext();
        var stepScopeA3 = mock(AbstractStepScope.class);
        when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA3);
        assertEntityPlacement(placementIterator.next(), "b", "1", "2", "3");
        placer.stepEnded(stepScopeA3);

        assertThat(placementIterator).hasNext();
        var stepScopeA4 = mock(AbstractStepScope.class);
        when(stepScopeA4.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA4);
        assertEntityPlacement(placementIterator.next(), "b", "8", "9");
        placer.stepEnded(stepScopeA4);

        assertThat(placementIterator).isExhausted();
        placer.phaseEnded(phaseScopeA);

        placer.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 1, 4);
        verifyPhaseLifecycle(primaryValueSelector, 1, 1, 4);
        verifyPhaseLifecycle(secondaryValueSelector, 1, 1, 4);
    }

    @Test
    void cartesianProductMoveSelector() {
        EntitySelector<TestdataMultiVarSolution> entitySelector =
                SelectorTestUtils.mockEntitySelector(TestdataMultiVarEntity.class,
                        new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b"));
        MimicRecordingEntitySelector<TestdataMultiVarSolution> recordingEntitySelector =
                new MimicRecordingEntitySelector<>(entitySelector);
        ValueSelector<TestdataMultiVarSolution> primaryValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataMultiVarEntity.class, "primaryValue",
                new TestdataValue("1"), new TestdataValue("2"), new TestdataValue("3"));
        ValueSelector<TestdataMultiVarSolution> secondaryValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataMultiVarEntity.class, "secondaryValue",
                new TestdataValue("8"), new TestdataValue("9"));

        var moveSelectorList = new ArrayList<MoveSelector<TestdataMultiVarSolution>>(2);
        moveSelectorList.add(new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector),
                primaryValueSelector,
                false));
        moveSelectorList.add(new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector),
                secondaryValueSelector,
                false));
        var moveSelector = new CartesianProductMoveSelector<>(moveSelectorList, true, false);
        var placer = new QueuedEntityPlacer<>(null, null, recordingEntitySelector,
                Collections.singletonList(moveSelector));

        var solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        var phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        Iterator<Placement<TestdataMultiVarSolution>> placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "a->1+a->8", "a->1+a->9", "a->2+a->8", "a->2+a->9", "a->3+a->8", "a->3+a->9");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "b->1+b->8", "b->1+b->9", "b->2+b->8", "b->2+b->9", "b->3+b->8", "b->3+b->9");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).isExhausted();
        placer.phaseEnded(phaseScopeA);

        placer.solvingEnded(solverScope);

        verifyPhaseLifecycle(entitySelector, 1, 1, 2);
        verifyPhaseLifecycle(primaryValueSelector, 1, 1, 2);
        verifyPhaseLifecycle(secondaryValueSelector, 1, 1, 2);
    }

    @Test
    void copy() {
        EntitySelector<TestdataMultiVarSolution> entitySelector =
                SelectorTestUtils.mockEntitySelector(TestdataMultiVarEntity.class,
                        new TestdataMultiVarEntity("a"), new TestdataMultiVarEntity("b"));
        MimicRecordingEntitySelector<TestdataMultiVarSolution> recordingEntitySelector =
                new MimicRecordingEntitySelector<>(entitySelector);
        ValueSelector<TestdataMultiVarSolution> primaryValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataMultiVarEntity.class, "primaryValue",
                new TestdataValue("1"), new TestdataValue("2"), new TestdataValue("3"));
        ValueSelector<TestdataMultiVarSolution> secondaryValueSelector = SelectorTestUtils.mockValueSelector(
                TestdataMultiVarEntity.class, "secondaryValue",
                new TestdataValue("8"), new TestdataValue("9"));
        var moveSelectorList = new ArrayList<MoveSelector<TestdataMultiVarSolution>>(2);
        moveSelectorList.add(new ChangeMoveSelector<>(
                new MimicReplayingEntitySelector<>(recordingEntitySelector),
                primaryValueSelector,
                false));
        moveSelectorList.add(
                new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector), secondaryValueSelector,
                        false));
        var moveSelector = new CartesianProductMoveSelector<>(moveSelectorList, true, false);
        var factory = mock(EntityPlacerFactory.class);
        var configPolicy = mock(HeuristicConfigPolicy.class);
        assertThatThrownBy(
                () -> new QueuedEntityPlacer<>(null, null, recordingEntitySelector, Collections.singletonList(moveSelector))
                        .copy())
                .hasMessage("The entity placer cannot be copied.");
        var placer = new QueuedEntityPlacer<TestdataMultiVarSolution>(factory, configPolicy, recordingEntitySelector,
                Collections.singletonList(moveSelector));
        placer.copy();
        verify(factory, times(1)).buildEntityPlacer(any());
    }

}
