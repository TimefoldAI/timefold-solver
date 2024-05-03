package ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class SimulatedAnnealingAcceptorTest extends AbstractAcceptorTest {

    @Test
    void lateAcceptanceSize() {
        var acceptor = new SimulatedAnnealingAcceptor<>();
        acceptor.setStartingTemperature(SimpleScore.of(200));

        var solverScope = new SolverScope<>();
        solverScope.setBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(SimpleScore.of(-1000));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setTimeGradient(0.0);
        acceptor.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -500);
        solverScope.setWorkingRandom(new TestRandom(0.3));
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1300))).isFalse();
        solverScope.setWorkingRandom(new TestRandom(0.3));
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1200))).isTrue();
        solverScope.setWorkingRandom(new TestRandom(0.4));
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1200))).isFalse();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore(moveScope0.getScore());
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        stepScope1.setTimeGradient(0.5);
        acceptor.stepStarted(stepScope1);
        var moveScope1 = buildMoveScope(stepScope1, -800);
        solverScope.setWorkingRandom(new TestRandom(0.13));
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -700))).isTrue();
        solverScope.setWorkingRandom(new TestRandom(0.14));
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -700))).isFalse();
        solverScope.setWorkingRandom(new TestRandom(0.04));
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        solverScope.setWorkingRandom(new TestRandom(0.01, 0.01));
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        stepScope2.setTimeGradient(1.0);
        acceptor.stepStarted(stepScope2);
        var moveScope2 = buildMoveScope(stepScope1, -400);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -800))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -801))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -1200))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -700))).isTrue();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void negativeSimulatedAnnealingSize() {
        var acceptor = new SimulatedAnnealingAcceptor<>();
        acceptor.setStartingTemperature(HardMediumSoftScore.of(1, -1, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }

}
