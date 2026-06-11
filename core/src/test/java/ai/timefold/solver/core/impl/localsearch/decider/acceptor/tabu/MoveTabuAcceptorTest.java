package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size.FixedTabuSizeStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class MoveTabuAcceptorTest {

    @Test
    void tabuSize() {
        var acceptor = new MoveTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));

        var m0 = mock(Move.class);
        var m1 = mock(Move.class);
        var m2 = mock(Move.class);
        var m3 = mock(Move.class);
        var m4 = mock(Move.class);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.ZERO);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        // Step 0: map is empty — all moves accepted
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, m1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, m2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, m3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, m4))).isTrue();
        stepScope0.setStep(m1);
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // Step 1: map={m1:0}; m1 tabuStepCount=1 ≤ 2 → tabu
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m4))).isTrue();
        stepScope1.setStep(m2);
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // Step 2: map={m1:0,m2:1}; both tabu
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, m1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, m2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, m3))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, m4))).isTrue();
        // adjustTabuList(2,[m3]): m1 tabuStepCount=2 ≥ 2 → removed; map={m2:1,m3:2}
        stepScope2.setStep(m3);
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // Step 3: map={m2:1,m3:2}; m1 expired
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, m1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, m2))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, m3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, m4))).isTrue();
        // adjustTabuList(3,[m4]): m2 tabuStepCount=2 ≥ 2 → removed; map={m3:2,m4:3}
        stepScope3.setStep(m4);
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // Step 4: map={m3:2,m4:3}; m2 expired
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, m1))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, m2))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, m3))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, m4))).isFalse();
        stepScope4.setStep(m0);
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void aspiration() {
        var acceptor = new MoveTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));
        acceptor.setAspirationEnabled(true);

        var m0 = mock(Move.class);
        var m1 = mock(Move.class);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-100));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setStep(m1);
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -120, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -20, m0))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -120, m1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -20, m1))).isTrue();

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void fadingTabuSize() {
        var acceptor = new MoveTabuAcceptor<>("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy<>(2));
        acceptor.setFadingTabuSizeStrategy(new FixedTabuSizeStrategy<>(4));

        var m0 = mock(Move.class);
        var m1 = mock(Move.class);

        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.ZERO);
        solverScope.setWorkingRandom(new TestRandom(new double[0]));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        acceptor.phaseStarted(phaseScope);

        // Step 0: tabu m1 at stepIndex=0
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setStep(m1);
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // Steps 1-2: hard tabu — no random consumed
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m1))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, m0))).isTrue();
        stepScope1.setStep(m0);
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, m1))).isFalse();
        stepScope2.setStep(m0);
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // Step 3: fadingCount=1, acceptChance=0.6; random=0.3 → accepted
        solverScope.setWorkingRandom(new TestRandom(0.3));
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, m1))).isTrue();
        stepScope3.setStep(m0);
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // Step 4: fadingCount=2, acceptChance=0.4; random=0.5 → rejected
        solverScope.setWorkingRandom(new TestRandom(0.5));
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, m1))).isFalse();
        stepScope4.setStep(m0);
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // Step 5: fadingCount=3, acceptChance=0.2; random=0.1 → accepted
        solverScope.setWorkingRandom(new TestRandom(0.1));
        var stepScope5 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, m1))).isTrue();
        stepScope5.setStep(m0);
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        // Step 6: fadingCount=4, acceptChance=0.0; random consumed but always false
        // adjustTabuList removes m1 (tabuStepCount=6 ≥ totalTabuListSize=6)
        solverScope.setWorkingRandom(new TestRandom(0.99));
        var stepScope6 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope6, m1))).isFalse();
        stepScope6.setStep(m0);
        acceptor.stepEnded(stepScope6);
        phaseScope.setLastCompletedStepScope(stepScope6);

        // Step 7: m1 expired, no random consumed
        solverScope.setWorkingRandom(new TestRandom(new double[0]));
        var stepScope7 = new LocalSearchStepScope<>(phaseScope);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope7, m1))).isTrue();

        acceptor.phaseEnded(phaseScope);
    }

    private static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(
            LocalSearchStepScope<Solution_> stepScope, Move<Solution_> move) {
        var moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setInitializedScore(SimpleScore.of(0));
        return moveScope;
    }

    private static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(
            LocalSearchStepScope<Solution_> stepScope, int score, Move<Solution_> move) {
        var moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setInitializedScore(SimpleScore.of(score));
        return moveScope;
    }

}
