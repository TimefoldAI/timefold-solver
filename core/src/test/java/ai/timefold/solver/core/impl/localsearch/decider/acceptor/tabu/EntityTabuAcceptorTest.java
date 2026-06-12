package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size.FixedTabuSizeStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class EntityTabuAcceptorTest {

    @Test
    void tabuSize() {
        var acceptor = new EntityTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));
        acceptor.setAspirationEnabled(true);

        var e0 = new TestdataEntity("e0");
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var e3 = new TestdataEntity("e3");
        var e4 = new TestdataEntity("e4");

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.ZERO);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        var moveScope1 = buildMoveScope(stepScope0, e1);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e0))).isTrue();
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e4))).isTrue();
        // repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e2))).isTrue();
        stepScope0.setStep(moveScope1.getMove());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        var moveScope2 = buildMoveScope(stepScope1, e2);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e1))).isFalse();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e4))).isTrue();
        // repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e2))).isTrue();
        stepScope1.setStep(moveScope2.getMove());
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        var moveScope4 = buildMoveScope(stepScope2, e4);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e3))).isTrue();
        assertThat(acceptor.isAccepted(moveScope4)).isTrue();
        // repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e2))).isFalse();
        stepScope2.setStep(moveScope4.getMove());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        var moveScope3 = buildMoveScope(stepScope3, e3);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e2))).isFalse();
        assertThat(acceptor.isAccepted(moveScope3)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e4))).isFalse();
        // repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e2))).isFalse();
        stepScope3.setStep(moveScope3.getMove());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        var moveScope1Again = buildMoveScope(stepScope4, e1);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, e0))).isTrue();
        assertThat(acceptor.isAccepted(moveScope1Again)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, e4))).isFalse();
        // repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, e2))).isTrue();
        stepScope4.setStep(moveScope1Again.getMove());
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void tabuSizeMultipleEntitiesPerStep() {
        var acceptor = new EntityTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));
        acceptor.setAspirationEnabled(true);

        var e0 = new TestdataEntity("e0");
        var e1 = new TestdataEntity("e1");
        var e2 = new TestdataEntity("e2");
        var e3 = new TestdataEntity("e3");
        var e4 = new TestdataEntity("e4");

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.ZERO);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e0, e1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e0, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e0, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e0, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e1, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e1, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e1, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e2, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e2, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, e3, e4))).isTrue();
        stepScope0.setStep(buildMoveScope(stepScope0, e0, e2).getMove());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e1, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e1, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e1, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e2, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e2, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e3, e4))).isTrue();
        stepScope1.setStep(buildMoveScope(stepScope1, e1).getMove());
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e0))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e4))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e0, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e0, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e0, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e0, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e1, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e1, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e1, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e2, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e2, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e3, e4))).isTrue();
        stepScope2.setStep(buildMoveScope(stepScope2, e3, e4).getMove());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e0, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e0, e2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e0, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e0, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e1, e2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e1, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e1, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e2, e3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e2, e4))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e3, e4))).isFalse();
        stepScope3.setStep(buildMoveScope(stepScope3, e0).getMove());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void aspiration() {
        var acceptor = new EntityTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));
        acceptor.setAspirationEnabled(true);

        var e0 = new TestdataEntity("e0");
        var e1 = new TestdataEntity("e1");

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-100));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setStep(buildMoveScope(stepScope0, e1).getMove());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -120, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -20, e0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -120, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -20, e1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -120, e0, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -20, e0, e1))).isTrue();
        stepScope1.setStep(buildMoveScope(stepScope1, -20, e1).getMove());
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void fadingTabuSize() {
        var acceptor = new EntityTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));
        acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(4));

        var e0 = new TestdataEntity("e0");
        var e1 = new TestdataEntity("e1");

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.ZERO);
        solverScope.setWorkingRandom(new TestRandom(new double[0]));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        // Step 0: tabu e1 at stepIndex=0
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setStep(buildMoveScope(stepScope0, e1).getMove());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // Steps 1-2: hard tabu (tabuStepCount 1,2 ≤ 2) — no random consumed
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, e0))).isTrue();
        stepScope1.setStep(buildMoveScope(stepScope1, e0).getMove());
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, e1))).isFalse();
        stepScope2.setStep(buildMoveScope(stepScope2, e0).getMove());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // Step 3: fading zone, fadingCount=1, acceptChance=0.4; random=0.3 → accepted
        solverScope.setWorkingRandom(new TestRandom(0.3));
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, e1))).isTrue();
        stepScope3.setStep(buildMoveScope(stepScope3, e0).getMove());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // Step 4: fadingCount=2, acceptChance=0.6; random=0.5 → accepted
        solverScope.setWorkingRandom(new TestRandom(0.5));
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, e1))).isTrue();
        stepScope4.setStep(buildMoveScope(stepScope4, e0).getMove());
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // Step 5: fadingCount=3, acceptChance=0.8; random=0.1 → accepted
        solverScope.setWorkingRandom(new TestRandom(0.1));
        var stepScope5 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, e1))).isTrue();
        stepScope5.setStep(buildMoveScope(stepScope5, e0).getMove());
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        // Step 6: fadingCount=4, acceptChance=1.0; random not consumed and accepted
        // adjustTabuList removes e1 (tabuStepCount=6 ≥ totalTabuListSize=6)
        solverScope.setWorkingRandom(new TestRandom(new double[0]));
        var stepScope6 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope6, e1))).isTrue();
        stepScope6.setStep(buildMoveScope(stepScope6, e0).getMove());
        acceptor.stepEnded(stepScope6);
        phaseScope.setLastCompletedStepScope(stepScope6);

        // Step 7: e1 expired, no random consumed
        solverScope.setWorkingRandom(new TestRandom(new double[0]));
        var stepScope7 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope7, e1))).isTrue();

        acceptor.phaseEnded(phaseScope);
    }

    private static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(LocalSearchStepScope<Solution_> stepScope,
            TestdataEntity... entities) {
        return buildMoveScope(stepScope, 0, entities);
    }

    private static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(
            LocalSearchStepScope<Solution_> stepScope, int score, TestdataEntity... entities) {
        var move = mock(Move.class);
        when(move.getPlanningEntities()).thenReturn(Arrays.asList(entities));
        var moveScope = new LocalSearchMoveScope<Solution_>(stepScope, 0, move);
        moveScope.setInitializedScore(SimpleScore.of(score));
        return moveScope;
    }

}
