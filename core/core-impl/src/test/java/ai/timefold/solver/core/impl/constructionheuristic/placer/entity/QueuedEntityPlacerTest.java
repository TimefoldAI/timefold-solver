package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.impl.constructionheuristic.placer.entity.PlacementAssertions.assertEntityPlacement;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertAllCodesOfIterator;
import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.verifyPhaseLifecycle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.constructionheuristic.placer.Placement;
import ai.timefold.solver.core.impl.constructionheuristic.placer.QueuedEntityPlacer;
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
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.TestdataValue;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarEntity;
import ai.timefold.solver.core.impl.testdata.domain.multivar.TestdataMultiVarSolution;

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

        MoveSelector<TestdataSolution> moveSelector =
                new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector), valueSelector, false);
        QueuedEntityPlacer<TestdataSolution> placer =
                new QueuedEntityPlacer<>(recordingEntitySelector, Collections.singletonList(moveSelector));

        SolverScope<TestdataSolution> solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        AbstractPhaseScope<TestdataSolution> phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        Iterator<Placement<TestdataSolution>> placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataSolution> stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertEntityPlacement(placementIterator.next(), "a", "1", "2");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataSolution> stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertEntityPlacement(placementIterator.next(), "b", "1", "2");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataSolution> stepScopeA3 = mock(AbstractStepScope.class);
        when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA3);
        assertEntityPlacement(placementIterator.next(), "c", "1", "2");
        placer.stepEnded(stepScopeA3);

        assertThat(placementIterator).isExhausted();
        placer.phaseEnded(phaseScopeA);

        AbstractPhaseScope<TestdataSolution> phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeB);
        placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataSolution> stepScopeB1 = mock(AbstractStepScope.class);
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

        List<MoveSelector<TestdataMultiVarSolution>> moveSelectorList = new ArrayList<>(2);
        moveSelectorList.add(new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector),
                primaryValueSelector,
                false));
        moveSelectorList.add(new ChangeMoveSelector<>(
                new MimicReplayingEntitySelector<>(recordingEntitySelector),
                secondaryValueSelector,
                false));
        QueuedEntityPlacer<TestdataMultiVarSolution> placer =
                new QueuedEntityPlacer<>(recordingEntitySelector, moveSelectorList);

        SolverScope<TestdataMultiVarSolution> solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        AbstractPhaseScope<TestdataMultiVarSolution> phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        Iterator<Placement<TestdataMultiVarSolution>> placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataMultiVarSolution> stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertEntityPlacement(placementIterator.next(), "a", "1", "2", "3");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataMultiVarSolution> stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertEntityPlacement(placementIterator.next(), "a", "8", "9");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataMultiVarSolution> stepScopeA3 = mock(AbstractStepScope.class);
        when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA3);
        assertEntityPlacement(placementIterator.next(), "b", "1", "2", "3");
        placer.stepEnded(stepScopeA3);

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataMultiVarSolution> stepScopeA4 = mock(AbstractStepScope.class);
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

        List<MoveSelector<TestdataMultiVarSolution>> moveSelectorList = new ArrayList<>(2);
        moveSelectorList.add(new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector),
                primaryValueSelector,
                false));
        moveSelectorList.add(new ChangeMoveSelector<>(new MimicReplayingEntitySelector<>(recordingEntitySelector),
                secondaryValueSelector,
                false));
        MoveSelector<TestdataMultiVarSolution> moveSelector = new CartesianProductMoveSelector<>(moveSelectorList, true, false);
        QueuedEntityPlacer<TestdataMultiVarSolution> placer = new QueuedEntityPlacer<>(recordingEntitySelector,
                Collections.singletonList(moveSelector));

        SolverScope<TestdataMultiVarSolution> solverScope = mock(SolverScope.class);
        placer.solvingStarted(solverScope);

        AbstractPhaseScope<TestdataMultiVarSolution> phaseScopeA = mock(AbstractPhaseScope.class);
        when(phaseScopeA.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeA);
        Iterator<Placement<TestdataMultiVarSolution>> placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataMultiVarSolution> stepScopeA1 = mock(AbstractStepScope.class);
        when(stepScopeA1.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA1);
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "a->1+a->8", "a->1+a->9", "a->2+a->8", "a->2+a->9", "a->3+a->8", "a->3+a->9");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        AbstractStepScope<TestdataMultiVarSolution> stepScopeA2 = mock(AbstractStepScope.class);
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

}
