package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size.FixedTabuSizeStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class EntityTabuAcceptorTest {

    @Test
    void tabuSize() {
        EntityTabuAcceptor acceptor = new EntityTabuAcceptor("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy(2));
        acceptor.setAspirationEnabled(true);

        TestdataEntity e0 = new TestdataEntity("e0");
        TestdataEntity e1 = new TestdataEntity("e1");
        TestdataEntity e2 = new TestdataEntity("e2");
        TestdataEntity e3 = new TestdataEntity("e3");
        TestdataEntity e4 = new TestdataEntity("e4");

        SolverScope<TestdataSolution> solverScope = new SolverScope<>();
        solverScope.setBestScore(SimpleScore.of(0));
        LocalSearchPhaseScope<TestdataSolution> phaseScope = new LocalSearchPhaseScope<>(solverScope);
        acceptor.phaseStarted(phaseScope);

        LocalSearchStepScope<TestdataSolution> stepScope0 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope1 = buildMoveScope(stepScope0, e1);
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

        LocalSearchStepScope<TestdataSolution> stepScope1 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope2 = buildMoveScope(stepScope1, e2);
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

        LocalSearchStepScope<TestdataSolution> stepScope2 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope4 = buildMoveScope(stepScope2, e4);
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

        LocalSearchStepScope<TestdataSolution> stepScope3 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope3 = buildMoveScope(stepScope3, e3);
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

        LocalSearchStepScope<TestdataSolution> stepScope4 = new LocalSearchStepScope<>(phaseScope);
        LocalSearchMoveScope<TestdataSolution> moveScope1Again = buildMoveScope(stepScope4, e1);
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
        EntityTabuAcceptor acceptor = new EntityTabuAcceptor("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy(2));
        acceptor.setAspirationEnabled(true);

        TestdataEntity e0 = new TestdataEntity("e0");
        TestdataEntity e1 = new TestdataEntity("e1");
        TestdataEntity e2 = new TestdataEntity("e2");
        TestdataEntity e3 = new TestdataEntity("e3");
        TestdataEntity e4 = new TestdataEntity("e4");

        SolverScope<TestdataSolution> solverScope = new SolverScope<>();
        solverScope.setBestScore(SimpleScore.of(0));
        LocalSearchPhaseScope<TestdataSolution> phaseScope = new LocalSearchPhaseScope<>(solverScope);
        acceptor.phaseStarted(phaseScope);

        LocalSearchStepScope<TestdataSolution> stepScope0 = new LocalSearchStepScope<>(phaseScope);
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

        LocalSearchStepScope<TestdataSolution> stepScope1 = new LocalSearchStepScope<>(phaseScope);
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

        LocalSearchStepScope<TestdataSolution> stepScope2 = new LocalSearchStepScope<>(phaseScope);
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

        LocalSearchStepScope<TestdataSolution> stepScope3 = new LocalSearchStepScope<>(phaseScope);
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
        EntityTabuAcceptor acceptor = new EntityTabuAcceptor("");
        acceptor.setTabuSizeStrategy(new FixedTabuSizeStrategy(2));
        acceptor.setAspirationEnabled(true);

        TestdataEntity e0 = new TestdataEntity("e0");
        TestdataEntity e1 = new TestdataEntity("e1");

        SolverScope<TestdataSolution> solverScope = new SolverScope<>();
        solverScope.setBestScore(SimpleScore.of(-100));
        LocalSearchPhaseScope<TestdataSolution> phaseScope = new LocalSearchPhaseScope<>(solverScope);
        acceptor.phaseStarted(phaseScope);

        LocalSearchStepScope<TestdataSolution> stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setStep(buildMoveScope(stepScope0, e1).getMove());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        LocalSearchStepScope<TestdataSolution> stepScope1 = new LocalSearchStepScope<>(phaseScope);
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

    private <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(
            LocalSearchStepScope<Solution_> stepScope, TestdataEntity... entities) {
        return buildMoveScope(stepScope, 0, entities);
    }

    private <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(
            LocalSearchStepScope<Solution_> stepScope, int score, TestdataEntity... entities) {
        Move move = mock(Move.class);
        when(move.getPlanningEntities()).thenReturn(Arrays.asList(entities));
        LocalSearchMoveScope<Solution_> moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setScore(SimpleScore.of(score));
        return moveScope;
    }

}
