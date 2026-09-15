package ai.timefold.solver.core.impl.localsearch.decider.forager.finalist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.junit.jupiter.api.Test;

class HighestScoreFinalistPodiumTest {
    @Test
    void alwaysPickImprovingMove() {
        var finalistPodium = new HighestScoreFinalistPodium<>();

        // Reference score is [0, -2, -3]
        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(HardMediumSoftScore.of(-0, -2, -3));
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

    @Test
    void neverPickStructurallyFlawedMove() {
        var finalistPodium = new HighestScoreFinalistPodium<>();

        // Reference score is [0, -2, -3]
        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(HardMediumSoftScore.of(-0, -2, -3));
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var lastCompletedStepScope = new LocalSearchStepScope<>(phaseScope, -1);
        lastCompletedStepScope.setScore(solverScope.getBestScore());
        phaseScope.setLastCompletedStepScope(lastCompletedStepScope);
        finalistPodium.phaseStarted(phaseScope);

        // Have two moves, scores [flawed, -1, -1, 3] and [flawed, 0, -2, -1]
        var stepScope0 = new LocalSearchStepScope<>(phaseScope);
        finalistPodium.stepStarted(stepScope0);
        var moveScope0 = buildMoveScope(stepScope0, -1, -1, -3, true);
        finalistPodium.addMove(moveScope0);
        var moveScope1 = buildMoveScope(stepScope0, 0, -2, -1, true);
        finalistPodium.addMove(moveScope1);

        // The list is empty as the moves are structurally flawed
        assertThat(finalistPodium.getFinalistList()).isEmpty();
    }

    protected static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(LocalSearchStepScope<Solution_> stepScope,
            int hardScore, int mediumScore, int softScore) {
        return buildMoveScope(stepScope, hardScore, mediumScore, softScore, false);
    }

    protected static <Solution_> LocalSearchMoveScope<Solution_> buildMoveScope(LocalSearchStepScope<Solution_> stepScope,
            int hardScore, int mediumScore, int softScore, boolean structurallyFlawed) {
        Move<Solution_> move = mock(Move.class);
        var moveScope = new LocalSearchMoveScope<>(stepScope, 0, move);
        moveScope.setInitializedScore(new HardMediumSoftScore(structurallyFlawed ? -1 : 0, hardScore, mediumScore, softScore));
        moveScope.setAccepted(true);
        return moveScope;
    }
}