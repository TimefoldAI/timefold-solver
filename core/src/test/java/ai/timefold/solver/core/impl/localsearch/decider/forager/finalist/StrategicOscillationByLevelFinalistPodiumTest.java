package ai.timefold.solver.core.impl.localsearch.decider.forager.finalist;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.extractSingleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class StrategicOscillationByLevelFinalistPodiumTest {

    @Test
    void referenceLastStepScore() {
        var finalistPodium = new StrategicOscillationByLevelFinalistPodium<>(false);

        var solverScope = new SolverScope<>();
        solverScope.setBestScore(HardSoftScore.of(-200, -5000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        finalistPodium.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -100, -7000);
        finalistPodium.addMove(buildMoveScope(stepScope0, -150, -2000));
        finalistPodium.addMove(moveScope0);
        finalistPodium.addMove(buildMoveScope(stepScope0, -100, -7100));
        finalistPodium.addMove(buildMoveScope(stepScope0, -200, -1000));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope0);
        stepScope0.setScore(moveScope0.getScore());
        finalistPodium.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope1);
        var moveScope1 = buildMoveScope(stepScope1, -120, -4000);
        finalistPodium.addMove(buildMoveScope(stepScope1, -100, -8000));
        finalistPodium.addMove(buildMoveScope(stepScope1, -100, -7000));
        finalistPodium.addMove(buildMoveScope(stepScope1, -150, -3000));
        finalistPodium.addMove(moveScope1);
        finalistPodium.addMove(buildMoveScope(stepScope1, -150, -2000));
        finalistPodium.addMove(buildMoveScope(stepScope1, -200, -1000));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope1);
        stepScope1.setScore(moveScope1.getScore());
        finalistPodium.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope2);
        var moveScope2 = buildMoveScope(stepScope2, -150, -1000);
        finalistPodium.addMove(buildMoveScope(stepScope2, -120, -4000));
        finalistPodium.addMove(buildMoveScope(stepScope2, -120, -5000));
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -3000));
        finalistPodium.addMove(moveScope2);
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -2000));
        finalistPodium.addMove(buildMoveScope(stepScope2, -160, -500));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope2);
        stepScope2.setScore(moveScope2.getScore());
        finalistPodium.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);
    }

    @Test
    void referenceBestScore() {
        var finalistPodium = new StrategicOscillationByLevelFinalistPodium<>(true);

        var solverScope = new SolverScope<>();
        solverScope.setBestScore(HardSoftScore.of(-200, -5000));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        finalistPodium.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -100, -7000);
        finalistPodium.addMove(buildMoveScope(stepScope0, -150, -2000));
        finalistPodium.addMove(moveScope0);
        finalistPodium.addMove(buildMoveScope(stepScope0, -100, -7100));
        finalistPodium.addMove(buildMoveScope(stepScope0, -200, -1000));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope0);
        stepScope0.setScore(moveScope0.getScore());
        finalistPodium.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);
        solverScope.setBestScore(stepScope0.getScore());

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope1);
        var moveScope1 = buildMoveScope(stepScope1, -120, -4000);
        finalistPodium.addMove(buildMoveScope(stepScope1, -100, -8000));
        finalistPodium.addMove(buildMoveScope(stepScope1, -100, -7000));
        finalistPodium.addMove(buildMoveScope(stepScope1, -150, -3000));
        finalistPodium.addMove(moveScope1);
        finalistPodium.addMove(buildMoveScope(stepScope1, -150, -2000));
        finalistPodium.addMove(buildMoveScope(stepScope1, -200, -1000));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope1);
        stepScope1.setScore(moveScope1.getScore());
        finalistPodium.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);
        // do not change bestScore

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope2);
        var moveScope2 = buildMoveScope(stepScope2, -110, -6000);
        finalistPodium.addMove(buildMoveScope(stepScope2, -110, -8000));
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -3000));
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -1000));
        finalistPodium.addMove(moveScope2);
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -2000));
        finalistPodium.addMove(buildMoveScope(stepScope2, -160, -500));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope2);
        stepScope2.setScore(moveScope2.getScore());
        finalistPodium.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);
        // do not change bestScore
    }

    protected static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(LocalSearchStepScope<Solution_> stepScope,
            int hardScore, int softScore) {
        Move<Solution_> move = mock(Move.class);
        var moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setScore(HardSoftScore.of(hardScore, softScore));
        moveScope.setAccepted(true);
        return moveScope;
    }

    @Test
    void referenceLastStepScore3Levels() {
        var finalistPodium = new StrategicOscillationByLevelFinalistPodium<>(false);

        var solverScope = new SolverScope<>();
        solverScope.setBestScore(HardMediumSoftScore.of(-200, -5000, -10));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        finalistPodium.phaseStarted(phaseScope);

        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -100, -7000, -20);
        finalistPodium.addMove(buildMoveScope(stepScope0, -150, -2000, -10));
        finalistPodium.addMove(moveScope0);
        finalistPodium.addMove(buildMoveScope(stepScope0, -100, -7100, -5));
        finalistPodium.addMove(buildMoveScope(stepScope0, -200, -1000, -10));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope0);
        stepScope0.setScore(moveScope0.getScore());
        finalistPodium.stepEnded(stepScope0);
        phaseScope.setLastCompletedStepScope(stepScope0);

        var stepScope1 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope1);
        var moveScope1 = buildMoveScope(stepScope1, -120, -4000, -40);
        finalistPodium.addMove(buildMoveScope(stepScope1, -100, -8000, -10));
        finalistPodium.addMove(buildMoveScope(stepScope1, -100, -7000, -30));
        finalistPodium.addMove(buildMoveScope(stepScope1, -150, -3000, -10));
        finalistPodium.addMove(moveScope1);
        finalistPodium.addMove(buildMoveScope(stepScope1, -150, -2000, -10));
        finalistPodium.addMove(buildMoveScope(stepScope1, -200, -1000, -10));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope1);
        stepScope1.setScore(moveScope1.getScore());
        finalistPodium.stepEnded(stepScope1);
        phaseScope.setLastCompletedStepScope(stepScope1);

        var stepScope2 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope2);
        var moveScope2 = buildMoveScope(stepScope2, -150, -1000, -20);
        finalistPodium.addMove(buildMoveScope(stepScope2, -120, -4000, -50));
        finalistPodium.addMove(buildMoveScope(stepScope2, -120, -5000, -10));
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -3000, -10));
        finalistPodium.addMove(moveScope2);
        finalistPodium.addMove(buildMoveScope(stepScope2, -150, -2000, -10));
        finalistPodium.addMove(buildMoveScope(stepScope2, -160, -500, -10));
        assertThat(extractSingleton(finalistPodium.getFinalistList())).isSameAs(moveScope2);
        stepScope2.setScore(moveScope2.getScore());
        finalistPodium.stepEnded(stepScope2);
        phaseScope.setLastCompletedStepScope(stepScope2);
    }

    @Test
    void alwaysPickImprovingMove() {
        var finalistPodium = new StrategicOscillationByLevelFinalistPodium<>(false);

        // Reference score is [0, -2, -3]
        var solverScope = new SolverScope<>();
        solverScope.setBestScore(HardMediumSoftScore.of(-0, -2, -3));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        finalistPodium.phaseStarted(phaseScope);

        // Have two moves, scores [-1, -1, 3] and [0, -2, -1]
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -1, -1, -3);
        finalistPodium.addMove(moveScope0);
        var moveScope1 = buildMoveScope(stepScope0, 0, -2, -1);
        finalistPodium.addMove(moveScope1);

        // The better is picked
        assertThat(finalistPodium.getFinalistList()).containsOnly(moveScope1);
    }

    protected static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(LocalSearchStepScope<Solution_> stepScope,
            int hardScore, int mediumScore, int softScore) {
        Move<Solution_> move = mock(Move.class);
        var moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setScore(HardMediumSoftScore.of(hardScore, mediumScore, softScore));
        moveScope.setAccepted(true);
        return moveScope;
    }

}
