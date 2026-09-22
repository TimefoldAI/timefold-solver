package ai.timefold.solver.console.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.definition.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class SolverDashboardTest {

    private static SolverDashboard<Object> dashboard(SolverScope<Object> solverScope) {
        return new SolverDashboard<>(Path.of("timefold-console-test.log"), null, solverScope);
    }

    @Test
    void stepEndedWithUnsetScoreShowsNotAvailable() {
        var solverScope = new SolverScope<>();
        var dashboard = dashboard(solverScope);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope = new LocalSearchStepScope<>(phaseScope, 0);

        dashboard.stepEnded(stepScope);

        assertThat(dashboard.buildSnapshot().stepScoreText()).isEqualTo("n/a");
    }

    @Test
    void stepEndedOnExhaustiveSearchUnwrapsTheExpandingNodeScore() {
        var solverScope = new SolverScope<>();
        var dashboard = dashboard(solverScope);
        var phaseScope = new ExhaustiveSearchPhaseScope<>(solverScope, 0);
        var stepScope = new ExhaustiveSearchStepScope<>(phaseScope, 0);
        var node = new ExhaustiveSearchNode<>(new ExhaustiveSearchLayer(0, null), null);
        node.setInitializedScore(SimpleScore.of(-7));
        stepScope.setExpandingNode(node);

        dashboard.stepEnded(stepScope);

        assertThat(dashboard.buildSnapshot().stepScoreText()).isEqualTo("-7");
    }

    @Test
    void stepEndedOnExhaustiveSearchWithAnUnscoredNodeShowsNotAvailable() {
        // BRUTE_FORCE never scores every node; AbstractStepScope.score stays unset in that case too.
        var solverScope = new SolverScope<>();
        var dashboard = dashboard(solverScope);
        var phaseScope = new ExhaustiveSearchPhaseScope<>(solverScope, 0);
        var stepScope = new ExhaustiveSearchStepScope<>(phaseScope, 0);
        stepScope.setExpandingNode(new ExhaustiveSearchNode<>(new ExhaustiveSearchLayer(0, null), null));

        dashboard.stepEnded(stepScope);

        assertThat(dashboard.buildSnapshot().stepScoreText()).isEqualTo("n/a");
    }

    @Test
    void stepEndedOnLocalSearchComputesTheAcceptedPercentage() {
        var solverScope = new SolverScope<>();
        var dashboard = dashboard(solverScope);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope = new LocalSearchStepScope<>(phaseScope, 0);
        stepScope.setInitializedScore(SimpleScore.of(-1));
        stepScope.setAcceptedMoveCount(3L);
        stepScope.setSelectedMoveCount(8L);

        dashboard.stepEnded(stepScope);

        assertThat(dashboard.buildSnapshot().acceptedPercentText()).isEqualTo("37.5 %");
    }

    @Test
    void stepEndedWithoutAcceptedOrSelectedCountsShowsDash() {
        var solverScope = new SolverScope<>();
        var dashboard = dashboard(solverScope);
        var phaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope = new LocalSearchStepScope<>(phaseScope, 0);
        stepScope.setInitializedScore(SimpleScore.of(-1));
        // Construction heuristic steps have a selected count but no acceptor, so no accepted count.

        dashboard.stepEnded(stepScope);

        assertThat(dashboard.buildSnapshot().acceptedPercentText()).isEqualTo("—");
    }

    @Test
    void phaseStartedClearsTheStepScoreFromThePreviousPhase() {
        var solverScope = new SolverScope<>();
        var dashboard = dashboard(solverScope);
        var localSearchPhaseScope = new LocalSearchPhaseScope<>(solverScope, 0);
        var stepScope = new LocalSearchStepScope<>(localSearchPhaseScope, 0);
        stepScope.setInitializedScore(SimpleScore.of(-1));
        dashboard.stepEnded(stepScope);
        assertThat(dashboard.buildSnapshot().stepScoreText()).isEqualTo("-1");

        @SuppressWarnings("unchecked")
        var nextPhaseScope = (AbstractPhaseScope<Object>) mock(AbstractPhaseScope.class);
        when(nextPhaseScope.getPhaseId()).thenReturn(EventProducerId.localSearch(1));
        dashboard.phaseStarted(nextPhaseScope);

        assertThat(dashboard.buildSnapshot().stepScoreText()).isEqualTo("n/a");
    }

    @Test
    void phaseEndedWithZeroElapsedTimeDoesNotDivideByZero() {
        var solverScope = new SolverScope<>();
        solverScope.setInitializedBestScore(SimpleScore.of(-1));
        var dashboard = dashboard(solverScope);

        @SuppressWarnings("unchecked")
        var phaseScope = (AbstractPhaseScope<Object>) mock(AbstractPhaseScope.class);
        when(phaseScope.getPhaseId()).thenReturn(EventProducerId.localSearch(0));
        doReturn(InnerScore.fullyAssigned(SimpleScore.of(-5))).when(phaseScope).getBestScore();
        // Every other AbstractPhaseScope method is unstubbed, so Mockito's defaults exercise the zero-steps edge case.

        dashboard.phaseEnded(phaseScope);

        assertThat(dashboard.buildSnapshot().finishedPhaseLines()).hasSize(1);
    }

    @Test
    void buildFinalSummaryTitleReflectsTheOutcome() {
        var solverScope = new SolverScope<>();
        solverScope.setScoreDirector(mock(InnerScoreDirector.class));
        var dashboard = dashboard(solverScope);
        assertThat(dashboard.buildFinalSummary().title()).isEqualTo("NO SOLUTION FOUND");

        // SimpleScore has no hard/soft split, so it can't demonstrate infeasibility; HardSoftScore can.
        solverScope.setBestScore(InnerScore.fullyAssigned(HardSoftScore.of(-1, 0)));
        assertThat(dashboard.buildFinalSummary().title()).isEqualTo("INFEASIBLE SOLUTION FOUND");

        solverScope.setBestScore(InnerScore.fullyAssigned(HardSoftScore.of(0, -1)));
        assertThat(dashboard.buildFinalSummary().title()).isEqualTo("FEASIBLE SOLUTION FOUND");
    }

    @Test
    void buildFinalSummaryAcceptanceIsNotAvailableWhenNoMoveWasSelected() {
        var solverScope = new SolverScope<>();
        solverScope.setScoreDirector(mock(InnerScoreDirector.class));
        var dashboard = dashboard(solverScope);

        assertThat(dashboard.buildFinalSummary().acceptanceText()).isEqualTo("n/a");
    }

    @Test
    void recordBestScoreSampleTracksEachScoreLevelSeparately() {
        @SuppressWarnings("unchecked")
        var scoreDirector = (InnerScoreDirector<Object, HardSoftScore>) mock(InnerScoreDirector.class);
        when(scoreDirector.getScoreDefinition()).thenReturn(new HardSoftScoreDefinition());
        var solverScope = new SolverScope<>();
        solverScope.setScoreDirector(scoreDirector);
        var dashboard = dashboard(solverScope);

        solverScope.setInitializedBestScore(HardSoftScore.of(-2, -100));
        dashboard.recordBestScoreSample();
        solverScope.setInitializedBestScore(HardSoftScore.of(-1, -900));
        dashboard.recordBestScoreSample();

        var snapshot = dashboard.buildSnapshot();
        assertThat(snapshot.scoreLevelLabels()).containsExactly("hard", "soft");
        // A hard-constraint repair paid for with a soft-score cost: hard only rises, soft only falls.
        assertThat(snapshot.bestScoreHistoryByLevel().get(0)).containsExactly(-2.0, -1.0);
        assertThat(snapshot.bestScoreHistoryByLevel().get(1)).containsExactly(-100.0, -900.0);
    }

}
