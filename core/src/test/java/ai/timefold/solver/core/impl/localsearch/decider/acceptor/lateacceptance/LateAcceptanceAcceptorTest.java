package ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptorTest;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stuckcriterion.StuckCriterion;
import ai.timefold.solver.core.impl.localsearch.decider.restart.AcceptorRestartStrategy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class LateAcceptanceAcceptorTest extends AbstractAcceptorTest {

    @Test
    void lateAcceptanceSize() {
        var restartStrategy = mock(StuckCriterion.class);
        when(restartStrategy.isSolverStuck(any())).thenReturn(false);
        var acceptor = new LateAcceptanceAcceptor<>(restartStrategy);
        acceptor.setLateAcceptanceSize(3);
        acceptor.setHillClimbingEnabled(false);

        var solverScope = new SolverScope<>();
        solverScope.setBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(SimpleScore.of(Integer.MIN_VALUE));
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lateScore = -1000
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        var moveScope0 = buildMoveScope(stepScope0, -500);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -800))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1000))).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore(moveScope0.getScore());
        acceptor.stepStarted(stepScope0);
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lateScore = -1000
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        var moveScope1 = buildMoveScope(stepScope1, -700);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -900))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -2000))).isFalse();
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1000))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1001))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lateScore = -1000
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        var moveScope2 = buildMoveScope(stepScope1, -400);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -900))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -1001))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -1000))).isTrue();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // lateScore = -500
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        var moveScope3 = buildMoveScope(stepScope1, -200);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -900))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -500))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -501))).isFalse();
        assertThat(acceptor.isAccepted(moveScope3)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -2000))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isFalse();
        stepScope3.setStep(moveScope3.getMove());
        stepScope3.setScore(moveScope3.getScore());
        solverScope.setBestScore(moveScope3.getScore());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // lateScore = -700 (not the best score of -500!)
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        var moveScope4 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -700))).isTrue();
        assertThat(acceptor.isAccepted(moveScope4)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -500))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -701))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -700))).isTrue();
        stepScope4.setStep(moveScope4.getMove());
        stepScope4.setScore(moveScope4.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // lateScore = -400
        var stepScope5 = new LocalSearchStepScope<>(phaseScope);
        var moveScope5 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -401))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -400))).isTrue();
        assertThat(acceptor.isAccepted(moveScope5)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -600))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -401))).isFalse();
        stepScope5.setStep(moveScope5.getMove());
        stepScope5.setScore(moveScope5.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void hillClimbingEnabled() {
        var restartStrategy = mock(StuckCriterion.class);
        when(restartStrategy.isSolverStuck(any())).thenReturn(false);
        var acceptor = new LateAcceptanceAcceptor<>(restartStrategy);
        acceptor.setLateAcceptanceSize(2);
        acceptor.setHillClimbingEnabled(true);

        var solverScope = new SolverScope<>();
        solverScope.setBestScore(SimpleScore.of(-1000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        acceptor.phaseStarted(phaseScope);

        // lateScore = -1000, lastCompletedStepScore = Integer.MIN_VALUE
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        var moveScope0 = buildMoveScope(stepScope0, -500);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -800))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -1000))).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope0.setStep(moveScope0.getMove());
        stepScope0.setScore(moveScope0.getScore());
        solverScope.setBestScore(moveScope0.getScore());
        acceptor.stepStarted(stepScope0);
        acceptor.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        // lateScore = -1000, lastCompletedStepScore = -500
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        var moveScope1 = buildMoveScope(stepScope1, -700);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -900))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -2000))).isFalse();
        assertThat(acceptor.isAccepted(moveScope1)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1000))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope1, -1001))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isTrue();
        stepScope1.setStep(moveScope1.getMove());
        stepScope1.setScore(moveScope1.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        // lateScore = -500, lastCompletedStepScore = -700
        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        var moveScope2 = buildMoveScope(stepScope1, -400);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -700))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -701))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope2, -600))).isTrue();
        assertThat(acceptor.isAccepted(moveScope2)).isTrue();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -700))).isTrue();
        stepScope2.setStep(moveScope2.getMove());
        stepScope2.setScore(moveScope2.getScore());
        solverScope.setBestScore(moveScope2.getScore());
        acceptor.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);

        // lateScore = -700, lastCompletedStepScore = -400
        var stepScope3 = new LocalSearchStepScope<>(phaseScope);
        var moveScope3 = buildMoveScope(stepScope1, -200);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -900))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -700))).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -701))).isFalse();
        assertThat(acceptor.isAccepted(moveScope3)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope3, -2000))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -900))).isFalse();
        stepScope3.setStep(moveScope3.getMove());
        stepScope3.setScore(moveScope3.getScore());
        solverScope.setBestScore(moveScope3.getScore());
        acceptor.stepEnded(stepScope3);
        phaseScope.setLastCompletedStepScope(stepScope3);

        // lateScore = -400 (not the best score of -200!), lastCompletedStepScore = -200
        var stepScope4 = new LocalSearchStepScope<>(phaseScope);
        var moveScope4 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -400))).isTrue();
        assertThat(acceptor.isAccepted(moveScope4)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -500))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope4, -401))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -400))).isTrue();
        stepScope4.setStep(moveScope4.getMove());
        stepScope4.setScore(moveScope4.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope4);
        phaseScope.setLastCompletedStepScope(stepScope4);

        // lateScore = -200, lastCompletedStepScore = -300
        var stepScope5 = new LocalSearchStepScope<>(phaseScope);
        var moveScope5 = buildMoveScope(stepScope1, -300);
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -301))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -400))).isFalse();
        assertThat(acceptor.isAccepted(moveScope5)).isTrue();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -2000))).isFalse();
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope5, -600))).isFalse();
        // Repeated call
        assertThat(acceptor.isAccepted(buildMoveScope(stepScope0, -301))).isFalse();
        stepScope5.setStep(moveScope5.getMove());
        stepScope5.setScore(moveScope5.getScore());
        // bestScore unchanged
        acceptor.stepEnded(stepScope5);
        phaseScope.setLastCompletedStepScope(stepScope5);

        acceptor.phaseEnded(phaseScope);
    }

    @Test
    void zeroLateAcceptanceSize() {
        var restartStrategy = mock(StuckCriterion.class);
        when(restartStrategy.isSolverStuck(any())).thenReturn(false);
        var acceptor = new LateAcceptanceAcceptor<>(restartStrategy);
        acceptor.setLateAcceptanceSize(0);
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }

    @Test
    void negativeLateAcceptanceSize() {
        var restartStrategy = mock(StuckCriterion.class);
        when(restartStrategy.isSolverStuck(any())).thenReturn(false);
        var acceptor = new LateAcceptanceAcceptor<>(restartStrategy);
        acceptor.setLateAcceptanceSize(-1);
        assertThatIllegalArgumentException().isThrownBy(() -> acceptor.phaseStarted(null));
    }

    @Test
    void triggerRestart() {
        var stuckCriterion = mock(StuckCriterion.class);
        when(stuckCriterion.isSolverStuck(any())).thenReturn(true);
        var acceptor = new LateAcceptanceAcceptor<>(stuckCriterion);
        acceptor.setLateAcceptanceSize(3);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        var moveScope0 = buildMoveScope(stepScope0, -2000);
        assertThat(acceptor.isAccepted(moveScope0)).isTrue();
        assertThat(phaseScope.isSolverStuck()).isTrue();
    }

    @Test
    void delayRestart() {
        var stuckCriterion = mock(StuckCriterion.class);
        var acceptor = new LateAcceptanceAcceptor<>(stuckCriterion);
        var restartStrategy = new AcceptorRestartStrategy(acceptor);
        acceptor.setLateAcceptanceSize(5);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1000));
        var moveScope0 = buildMoveScope(stepScope0, -3000);
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        acceptor.solvingStarted(solverScope);
        acceptor.phaseStarted(phaseScope);
        acceptor.stepStarted(stepScope0);
        assertThat(acceptor.isAccepted(moveScope0)).isFalse();

        // Delay because there aren't enough top scores in the queue
        assertThat(acceptor.bestScoreQueue.size()).isOne();
        restartStrategy.applyRestart(stepScope0);
        assertThat(acceptor.coefficient).isZero();

        // Delay because the diversity is still high on the first event
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-999));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-998));
        acceptor.previousScores[0] = SimpleScore.of(-1002);
        acceptor.previousScores[1] = SimpleScore.of(-1001);
        acceptor.previousScores[2] = SimpleScore.of(-1000);
        acceptor.previousScores[3] = SimpleScore.of(-999);
        acceptor.previousScores[4] = SimpleScore.of(-998);
        restartStrategy.applyRestart(stepScope0);
        assertThat(acceptor.coefficient).isZero();
    }

    @Test
    void delayOnlyFirstRestart() {
        var stuckCriterion = mock(StuckCriterion.class);
        var acceptor = new LateAcceptanceAcceptor<>(stuckCriterion);
        var restartStrategy = new AcceptorRestartStrategy(acceptor);
        acceptor.setLateAcceptanceSize(5);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1000));
        var moveScope0 = buildMoveScope(stepScope0, -3000);
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        acceptor.solvingStarted(solverScope);
        acceptor.phaseStarted(phaseScope);
        acceptor.stepStarted(stepScope0);
        assertThat(acceptor.isAccepted(moveScope0)).isFalse();

        // Delay because the diversity is still high on the first event
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-999));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-998));
        acceptor.previousScores[0] = SimpleScore.of(-1002);
        acceptor.previousScores[1] = SimpleScore.of(-1001);
        acceptor.previousScores[2] = SimpleScore.of(-1000);
        acceptor.previousScores[3] = SimpleScore.of(-999);
        acceptor.previousScores[4] = SimpleScore.of(-998);
        restartStrategy.applyRestart(stepScope0);
        assertThat(acceptor.coefficient).isZero();

        // Trigger the restart event as the diversity dropped
        acceptor.previousScores[0] = SimpleScore.of(-1002);
        acceptor.previousScores[1] = SimpleScore.of(-1002);
        acceptor.previousScores[2] = SimpleScore.of(-1002);
        acceptor.previousScores[3] = SimpleScore.of(-1002);
        acceptor.previousScores[4] = SimpleScore.of(-1002);
        restartStrategy.applyRestart(stepScope0);
        assertThat(acceptor.coefficient).isOne();
    }

    @Test
    void ensureDiversity() {
        var stuckCriterion = mock(StuckCriterion.class);
        var acceptor = new LateAcceptanceAcceptor<>(stuckCriterion);
        var restartStrategy = new AcceptorRestartStrategy(acceptor);
        acceptor.setLateAcceptanceSize(5);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1000));
        var moveScope0 = buildMoveScope(stepScope0, -3000);
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        acceptor.solvingStarted(solverScope);
        acceptor.phaseStarted(phaseScope);
        acceptor.stepStarted(stepScope0);
        assertThat(acceptor.isAccepted(moveScope0)).isFalse();

        acceptor.bestScoreQueue.addLast(SimpleScore.of(-999));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-998));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-997));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-996));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-995));
        restartStrategy.applyRestart(stepScope0);
        assertThat(acceptor.previousScores).hasSizeGreaterThan(5);
        assertThat(acceptor.previousScores).containsAnyOf(SimpleScore.of(-1000), SimpleScore.of(-999),
                SimpleScore.of(-998), SimpleScore.of(997), SimpleScore.of(-996), SimpleScore.of(-995));
    }

    @Test
    void reconfigureAfterImprovement() {
        var stuckCriterion = mock(StuckCriterion.class);
        var acceptor = new LateAcceptanceAcceptor<>(stuckCriterion);
        var restartStrategy = new AcceptorRestartStrategy(acceptor);
        acceptor.setLateAcceptanceSize(5);
        var solverScope = new SolverScope<>();
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        stepScope0.setScore(SimpleScore.of(-1000));
        var moveScope0 = buildMoveScope(stepScope0, -3000);
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(SimpleScore.of(-1000));
        acceptor.solvingStarted(solverScope);
        acceptor.phaseStarted(phaseScope);
        acceptor.stepStarted(stepScope0);
        assertThat(acceptor.isAccepted(moveScope0)).isFalse();

        // Apply restart
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-999));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-998));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-997));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-996));
        acceptor.bestScoreQueue.addLast(SimpleScore.of(-995));
        restartStrategy.applyRestart(stepScope0);
        assertThat(acceptor.coefficient).isOne();

        // Step that improves the best solution should not change the late elements list in the next restart
        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        stepScope1.setScore(SimpleScore.of(-900));
        acceptor.stepEnded(stepScope1);
        assertThat(acceptor.coefficient).isZero();
    }
}
