package ai.timefold.solver.core.impl.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testconstraint.DummyConstraintProvider;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class SolverContextManagerTest {

    private static final EnvironmentMode GLOBAL_MODE = EnvironmentMode.PHASE_ASSERT;

    private final ScoreDirectorFactory<TestdataSolution, SimpleScore> scoreDirectorFactory =
            new ScoreDirectorFactoryFactory<TestdataSolution, SimpleScore>(
                    new ScoreDirectorFactoryConfig().withConstraintProviderClass(DummyConstraintProvider.class))
                    .buildScoreDirectorFactory(GLOBAL_MODE, TestdataSolution.buildSolutionDescriptor());

    /**
     * A solver scope holding a freshly built score director for the global environment mode,
     * which is the state {@code DefaultSolverFactory.buildSolver} leaves behind.
     */
    private SolverScope<TestdataSolution> buildSolverScope() {
        var solverScope = new SolverScope<TestdataSolution>(Clock.systemDefaultZone());
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector =
                scoreDirectorFactory.createScoreDirectorBuilder(GLOBAL_MODE).withLookUpEnabled(true).build();
        scoreDirector.setWorkingSolution(TestdataSolution.generateSolution(3, 3));
        solverScope.setScoreDirector(scoreDirector);
        solverScope.setProblemChangeDirector(new DefaultProblemChangeDirector<>(scoreDirector));
        return solverScope;
    }

    private SolverContextManager<TestdataSolution, SimpleScore> buildManager(EnvironmentMode... phaseEnvironmentModes) {
        var phaseList = new ArrayList<Phase<TestdataSolution>>();
        for (var environmentMode : phaseEnvironmentModes) {
            Phase<TestdataSolution> phase = mock(Phase.class);
            when(phase.getEnvironmentMode()).thenReturn(environmentMode);
            phaseList.add(phase);
        }
        return new SolverContextManager<>(scoreDirectorFactory, new BestSolutionRecaller<>(), List.copyOf(phaseList));
    }

    private static void startPhase(SolverContextManager<TestdataSolution, SimpleScore> manager,
            SolverScope<TestdataSolution> solverScope, int phaseIndex) {
        manager.phaseStarted(new LocalSearchPhaseScope<>(solverScope, phaseIndex));
    }

    @Test
    void phaseWithTheSameEnvironmentModeKeepsTheScoreDirector() {
        var solverScope = buildSolverScope();
        var manager = buildManager(GLOBAL_MODE);
        var originalScoreDirector = solverScope.getScoreDirector();
        var originalProblemChangeDirector = solverScope.getProblemChangeDirector();

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 0);

        assertThat(solverScope.getScoreDirector()).isSameAs(originalScoreDirector);
        assertThat(solverScope.getProblemChangeDirector()).isSameAs(originalProblemChangeDirector);
        // Not closed; close() clears the working solution.
        assertThat(originalScoreDirector.getWorkingSolution()).isNotNull();
    }

    @Test
    void phaseWithAnotherEnvironmentModeSwapsInAScoreDirectorForThatMode() {
        var solverScope = buildSolverScope();
        var manager = buildManager(EnvironmentMode.FULL_ASSERT);
        var originalScoreDirector = solverScope.getScoreDirector();
        var originalProblemChangeDirector = solverScope.getProblemChangeDirector();
        var workingSolution = originalScoreDirector.getWorkingSolution();

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 0);

        var newScoreDirector = solverScope.getScoreDirector();
        var newProblemChangeDirector = solverScope.getProblemChangeDirector();
        assertThat(newScoreDirector).isNotSameAs(originalScoreDirector);
        assertThat(newProblemChangeDirector).isNotSameAs(originalProblemChangeDirector);
        assertThat(newScoreDirector.getEnvironmentMode()).isEqualTo(EnvironmentMode.FULL_ASSERT);
        // The working solution carries over rather than being re-cloned.
        assertThat(newScoreDirector.getWorkingSolution()).isSameAs(workingSolution);
    }

    @Test
    void aSwapKeepsTheConstraintMatchingTheMetricsRequire() {
        // A constraint match based metric enables matching regardless of the environment mode. The swapped-in
        // score director has to keep it, or the metric silently stops reporting after the first phase change.
        var solverScope = buildSolverScope();
        solverScope.setSolverMetricSet(EnumSet.of(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE));
        var manager = buildManager(GLOBAL_MODE, EnvironmentMode.NO_ASSERT);

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 1);

        // NO_ASSERT would disable matching on its own; only the metric keeps it on.
        assertThat(solverScope.getScoreDirector().getEnvironmentMode()).isEqualTo(EnvironmentMode.NO_ASSERT);
        assertThat(solverScope.getScoreDirector().getConstraintMatchPolicy())
                .isEqualTo(ConstraintMatchPolicy.ENABLED);
    }

    @Test
    void aSwapWithoutConstraintMatchMetricsLeavesMatchingOff() {
        var solverScope = buildSolverScope();
        solverScope.setSolverMetricSet(EnumSet.of(SolverMetric.BEST_SCORE));
        var manager = buildManager(GLOBAL_MODE, EnvironmentMode.NO_ASSERT);

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 1);

        assertThat(solverScope.getScoreDirector().getConstraintMatchPolicy())
                .isEqualTo(ConstraintMatchPolicy.DISABLED);
    }

    @Test
    void replacedScoreDirectorIsClosed() {
        var solverScope = buildSolverScope();
        var manager = buildManager(EnvironmentMode.FULL_ASSERT);
        var originalScoreDirector = solverScope.getScoreDirector();

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 0);

        // close() clears the working solution, so this is the observable proof it was released.
        assertThat(originalScoreDirector.getWorkingSolution()).isNull();
    }

    @Test
    void scoreCalculationCountCarriesOverToTheNewScoreDirector() {
        var solverScope = buildSolverScope();
        var manager = buildManager(EnvironmentMode.FULL_ASSERT);
        var originalScoreDirector = solverScope.getScoreDirector();
        originalScoreDirector.calculateScore();
        originalScoreDirector.calculateScore();
        var countBeforeSwap = originalScoreDirector.getCalculationCount();
        assertThat(countBeforeSwap).isPositive();

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 0);

        // Terminations count calculations across the whole solve, so the running total must not restart at zero.
        assertThat(solverScope.getScoreDirector().getCalculationCount()).isEqualTo(countBeforeSwap);
    }

    @Test
    void everyEnvironmentModeChangeGetsItsOwnScoreDirector() {
        var solverScope = buildSolverScope();
        // Back to the global mode for the third phase; score directors are not cached, so it must be a new instance.
        var manager = buildManager(GLOBAL_MODE, EnvironmentMode.FULL_ASSERT, GLOBAL_MODE);
        var originalScoreDirector = solverScope.getScoreDirector();

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 0);
        assertThat(solverScope.getScoreDirector()).isSameAs(originalScoreDirector);

        startPhase(manager, solverScope, 1);
        var fullAssertScoreDirector = solverScope.getScoreDirector();
        assertThat(fullAssertScoreDirector).isNotSameAs(originalScoreDirector);

        startPhase(manager, solverScope, 2);
        var restoredScoreDirector = solverScope.getScoreDirector();
        assertThat(restoredScoreDirector)
                .isNotSameAs(fullAssertScoreDirector)
                .isNotSameAs(originalScoreDirector);
        assertThat(restoredScoreDirector.getEnvironmentMode()).isEqualTo(GLOBAL_MODE);
    }

    @Test
    void solvingErrorClosesTheScoreDirectorInUse() {
        var solverScope = buildSolverScope();
        var manager = buildManager(EnvironmentMode.FULL_ASSERT);

        manager.solvingStarted(solverScope);
        startPhase(manager, solverScope, 0);
        var scoreDirectorInUse = solverScope.getScoreDirector();

        manager.solvingError(solverScope, new IllegalStateException("Boom"));

        // The solver's normal cleanup does not run on the failure path, so this is the only close.
        assertThat(scoreDirectorInUse.getWorkingSolution()).isNull();
    }

    @Test
    void solvingErrorReportsAFailedReleaseAsSuppressed() {
        var solverScope = new SolverScope<TestdataSolution>(Clock.systemDefaultZone());
        InnerScoreDirector<TestdataSolution, SimpleScore> scoreDirector = mock(InnerScoreDirector.class);
        var releaseFailure = new IllegalStateException("Releasing blew up");
        doThrow(releaseFailure).when(scoreDirector).close();
        solverScope.setScoreDirector(scoreDirector);
        var manager = buildManager(GLOBAL_MODE);
        var originalFailure = new IllegalStateException("The real failure");
        // The caller rethrows the original failure right after this returns, so a failure in here must not
        // take its place; it is attached instead, where it stays visible without hiding the real cause.
        assertThatCode(() -> manager.solvingError(solverScope, originalFailure)).doesNotThrowAnyException();
        assertThat(originalFailure.getSuppressed()).containsExactly(releaseFailure);
    }

    @Test
    void solvingErrorBeforeSolvingStartedDoesNotThrow() {
        var solverScope = buildSolverScope();
        var manager = buildManager(GLOBAL_MODE);

        // Solving can fail before solvingStarted() completes; the original exception must reach the caller
        // rather than being replaced by a NullPointerException from in here.
        assertThatCode(() -> manager.solvingError(solverScope, new IllegalStateException("Boom")))
                .doesNotThrowAnyException();
    }
}
