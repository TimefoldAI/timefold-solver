package ai.timefold.solver.core.impl.constructionheuristic.placer.entity;

import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfIterator;
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
import ai.timefold.solver.core.impl.constructionheuristic.placer.PooledEntityPlacer;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.move.DummyMove;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class PooledEntityPlacerTest {

    @Test
    void oneMoveSelector() {
        var moveSelector = SelectorTestUtils.mockMoveSelector(
                new DummyMove("a1"), new DummyMove("a2"), new DummyMove("b1"));

        var placer = new PooledEntityPlacer<>(null, null, moveSelector);

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
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "a1", "a2", "b1");
        placer.stepEnded(stepScopeA1);

        assertThat(placementIterator).hasNext();
        var stepScopeA2 = mock(AbstractStepScope.class);
        when(stepScopeA2.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA2);
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "a1", "a2", "b1");
        placer.stepEnded(stepScopeA2);

        assertThat(placementIterator).hasNext();
        var stepScopeA3 = mock(AbstractStepScope.class);
        when(stepScopeA3.getPhaseScope()).thenReturn(phaseScopeA);
        placer.stepStarted(stepScopeA3);
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "a1", "a2", "b1");
        placer.stepEnded(stepScopeA3);

        placer.phaseEnded(phaseScopeA);

        var phaseScopeB = mock(AbstractPhaseScope.class);
        when(phaseScopeB.getSolverScope()).thenReturn(solverScope);
        placer.phaseStarted(phaseScopeB);
        placementIterator = placer.iterator();

        assertThat(placementIterator).hasNext();
        var stepScopeB1 = mock(AbstractStepScope.class);
        when(stepScopeB1.getPhaseScope()).thenReturn(phaseScopeB);
        placer.stepStarted(stepScopeB1);
        assertAllCodesOfIterator(placementIterator.next().iterator(),
                "a1", "a2", "b1");
        placer.stepEnded(stepScopeB1);

        placer.phaseEnded(phaseScopeB);

        placer.solvingEnded(solverScope);

        verifyPhaseLifecycle(moveSelector, 1, 2, 4);
    }

    @Test
    void copy() {
        var moveSelector = SelectorTestUtils
                .mockMoveSelector(new DummyMove("a1"), new DummyMove("a2"), new DummyMove("b1"));
        var factory = mock(EntityPlacerFactory.class);
        var configPolicy = mock(HeuristicConfigPolicy.class);
        assertThatThrownBy(() -> new PooledEntityPlacer<>(null, null, moveSelector).copy())
                .hasMessage("The entity placer cannot be copied.");
        var placer = new PooledEntityPlacer<TestdataSolution>(factory, configPolicy, moveSelector);
        placer.copy();
        verify(factory, times(1)).buildEntityPlacer(any());
    }

}
