package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.localsearch.DefaultLocalSearchPhase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleSupport;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.exception.ScoreCorruptionException;
import ai.timefold.solver.core.impl.solver.exception.VariableCorruptionException;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see DefaultLocalSearchPhase
 */
public abstract class AbstractPhase<Solution_> implements Phase<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final int phaseIndex;
    protected final String logIndentation;

    // Called "phaseTermination" to clearly distinguish from "solverTermination" inside AbstractSolver.
    protected final PhaseTermination<Solution_> phaseTermination;

    protected final boolean assertPhaseScoreFromScratch;
    protected final boolean assertStepScoreFromScratch;
    protected final boolean assertExpectedStepScore;
    protected final boolean assertShadowVariablesAreNotStaleAfterStep;

    /** Used for {@link #addPhaseLifecycleListener(PhaseLifecycleListener)}. */
    protected PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    protected AbstractSolver<Solution_> solver;

    protected AbstractPhase(AbstractPhaseBuilder<Solution_> builder) {
        phaseIndex = builder.phaseIndex;
        logIndentation = builder.logIndentation;
        phaseTermination = builder.phaseTermination;
        assertPhaseScoreFromScratch = builder.assertPhaseScoreFromScratch;
        assertStepScoreFromScratch = builder.assertStepScoreFromScratch;
        assertExpectedStepScore = builder.assertExpectedStepScore;
        assertShadowVariablesAreNotStaleAfterStep = builder.assertShadowVariablesAreNotStaleAfterStep;
    }

    public int getPhaseIndex() {
        return phaseIndex;
    }

    public Termination<Solution_> getPhaseTermination() {
        return phaseTermination;
    }

    public AbstractSolver<Solution_> getSolver() {
        return solver;
    }

    public void setSolver(AbstractSolver<Solution_> solver) {
        this.solver = solver;
    }

    public boolean isAssertStepScoreFromScratch() {
        return assertStepScoreFromScratch;
    }

    public boolean isAssertExpectedStepScore() {
        return assertExpectedStepScore;
    }

    public boolean isAssertShadowVariablesAreNotStaleAfterStep() {
        return assertShadowVariablesAreNotStaleAfterStep;
    }

    public abstract String getPhaseTypeString();

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        phaseLifecycleSupport.fireSolvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        phaseLifecycleSupport.fireSolvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseScope.startingNow();
        phaseScope.reset();
        if (!isNested()) {
            solver.phaseStarted(phaseScope);
        }
        phaseTermination.phaseStarted(phaseScope);
        phaseLifecycleSupport.firePhaseStarted(phaseScope);
    }

    /**
     * Whether this phase is nested inside another phase.
     * Nested phases, such as ruin and recreate, must not notify the solver of their starting;
     * otherwise unimproved termination of local search would be reset every time the nested phase starts,
     * effectively disabling this termination.
     * Nested phases also do not collect step and phase metrics.
     *
     * @return false for nested phases, true for every other phase
     */
    protected boolean isNested() {
        return false;
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        if (!isNested()) {
            solver.phaseEnded(phaseScope);
        }
        phaseTermination.phaseEnded(phaseScope);
        phaseLifecycleSupport.firePhaseEnded(phaseScope);
        if (assertPhaseScoreFromScratch) {
            var score = phaseScope.getSolverScope().calculateScore();
            try {
                phaseScope.assertWorkingScoreFromScratch(score, getPhaseTypeString() + " phase ended");
            } catch (ScoreCorruptionException | VariableCorruptionException e) {
                throw new IllegalStateException("""
                        Solver corruption was detected. Solutions provided by this solver can not be trusted.
                        Corruptions typically arise from a bug in either your constraints or your variable listeners,
                        but they may also be caused by a rare solver bug.
                        Run your solver with %s %s to find out more information about the error \
                        and if you are convinced that the problem is not in your code, please report a bug to Timefold.
                        At your own risk, you may run your solver with %s or %s instead to ignore this error."""
                        .formatted(EnvironmentMode.class.getSimpleName(),
                                EnvironmentMode.FULL_ASSERT, EnvironmentMode.NO_ASSERT,
                                EnvironmentMode.NON_REPRODUCIBLE),
                        e);
            }
        }
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        if (!isNested()) {
            solver.stepStarted(stepScope);
        }
        phaseTermination.stepStarted(stepScope);
        phaseLifecycleSupport.fireStepStarted(stepScope);
    }

    protected <Score_ extends Score<Score_>> void calculateWorkingStepScore(AbstractStepScope<Solution_> stepScope,
            Object completedAction) {
        AbstractPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        Score_ score = phaseScope.calculateScore();
        stepScope.setScore(score);
        if (assertStepScoreFromScratch) {
            phaseScope.assertWorkingScoreFromScratch(score, completedAction);
        }
        if (assertShadowVariablesAreNotStaleAfterStep) {
            phaseScope.assertShadowVariablesAreNotStale(score, completedAction);
        }
    }

    @SuppressWarnings("unchecked")
    protected <Score_ extends Score<Score_>> void predictWorkingStepScore(AbstractStepScope<Solution_> stepScope,
            Object completedAction) {
        AbstractPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        // There is no need to recalculate the score, but we still need to set it
        phaseScope.getSolutionDescriptor().setScore(phaseScope.getWorkingSolution(), (Score_) stepScope.getScore());
        if (assertStepScoreFromScratch) {
            phaseScope.assertPredictedScoreFromScratch((Score_) stepScope.getScore(), completedAction);
        }
        if (assertExpectedStepScore) {
            phaseScope.assertExpectedWorkingScore((Score_) stepScope.getScore(), completedAction);
        }
        if (assertShadowVariablesAreNotStaleAfterStep) {
            phaseScope.assertShadowVariablesAreNotStale((Score_) stepScope.getScore(), completedAction);
        }
    }

    @Override
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        if (!isNested()) {
            solver.stepEnded(stepScope);
            collectMetrics(stepScope);
        }
        phaseTermination.stepEnded(stepScope);
        phaseLifecycleSupport.fireStepEnded(stepScope);
    }

    private static <Solution_> void collectMetrics(AbstractStepScope<Solution_> stepScope) {
        var solverScope = stepScope.getPhaseScope().getSolverScope();
        if (solverScope.isMetricEnabled(SolverMetric.STEP_SCORE) && stepScope.getScore().isSolutionInitialized()) {
            SolverMetric.registerScoreMetrics(SolverMetric.STEP_SCORE,
                    solverScope.getMonitoringTags(),
                    solverScope.getScoreDefinition(),
                    solverScope.getStepScoreMap(),
                    stepScope.getScore());
        }
    }

    @Override
    public void addPhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.addEventListener(phaseLifecycleListener);
    }

    @Override
    public void removePhaseLifecycleListener(PhaseLifecycleListener<Solution_> phaseLifecycleListener) {
        phaseLifecycleSupport.removeEventListener(phaseLifecycleListener);
    }

    // ************************************************************************
    // Assert methods
    // ************************************************************************

    protected void assertWorkingSolutionInitialized(AbstractPhaseScope<Solution_> phaseScope) {
        if (!phaseScope.getStartingScore().isSolutionInitialized()) {
            var scoreDirector = phaseScope.getScoreDirector();
            var solutionDescriptor = scoreDirector.getSolutionDescriptor();
            var workingSolution = scoreDirector.getWorkingSolution();
            var initializationStatistics = solutionDescriptor.computeInitializationStatistics(workingSolution);
            var uninitializedEntityCount = initializationStatistics.uninitializedEntityCount();
            if (uninitializedEntityCount > 0) {
                throw new IllegalStateException(
                        """
                                %s phase (%d) needs to start from an initialized solution, but there are (%d) uninitialized entities.
                                Maybe there is no Construction Heuristic configured before this phase to initialize the solution.
                                Or maybe the getter/setters of your planning variables in your domain classes aren't implemented correctly."""
                                .formatted(getPhaseTypeString(), phaseIndex, uninitializedEntityCount));
            }
            var unassignedValueCount = initializationStatistics.unassignedValueCount();
            if (unassignedValueCount > 0) {
                throw new IllegalStateException(
                        """
                                %s phase (%d) needs to start from an initialized solution, \
                                but planning list variable (%s) has (%d) unexpected unassigned values.
                                Maybe there is no Construction Heuristic configured before this phase to initialize the solution."""
                                .formatted(getPhaseTypeString(), phaseIndex, solutionDescriptor.getListVariableDescriptor(),
                                        unassignedValueCount));
            }
        }
    }

    public abstract static class AbstractPhaseBuilder<Solution_> {

        private final int phaseIndex;
        private final String logIndentation;
        private final PhaseTermination<Solution_> phaseTermination;

        private boolean assertPhaseScoreFromScratch = false;
        private boolean assertStepScoreFromScratch = false;
        private boolean assertExpectedStepScore = false;
        private boolean assertShadowVariablesAreNotStaleAfterStep = false;

        protected AbstractPhaseBuilder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination) {
            this.phaseIndex = phaseIndex;
            this.logIndentation = logIndentation;
            this.phaseTermination = phaseTermination;
        }

        public AbstractPhaseBuilder<Solution_> enableAssertions(EnvironmentMode environmentMode) {
            assertPhaseScoreFromScratch = environmentMode.isAsserted();
            assertStepScoreFromScratch = environmentMode.isFullyAsserted();
            assertExpectedStepScore = environmentMode.isIntrusivelyAsserted();
            assertShadowVariablesAreNotStaleAfterStep = environmentMode.isIntrusivelyAsserted();
            return this;
        }

        protected abstract AbstractPhase<Solution_> build();
    }
}
