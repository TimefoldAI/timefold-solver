package ai.timefold.solver.core.impl.phase;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.localsearch.DefaultLocalSearchPhase;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListener;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleSupport;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.solver.AbstractSolver;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
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
    protected final Termination<Solution_> phaseTermination;

    protected final boolean assertStepScoreFromScratch;
    protected final boolean assertExpectedStepScore;
    protected final boolean assertShadowVariablesAreNotStaleAfterStep;
    protected final boolean triggerFirstInitializedSolutionEvent;

    /** Used for {@link #addPhaseLifecycleListener(PhaseLifecycleListener)}. */
    protected PhaseLifecycleSupport<Solution_> phaseLifecycleSupport = new PhaseLifecycleSupport<>();

    protected AbstractSolver<Solution_> solver;

    protected AbstractPhase(Builder<Solution_> builder) {
        phaseIndex = builder.phaseIndex;
        logIndentation = builder.logIndentation;
        phaseTermination = builder.phaseTermination;
        assertStepScoreFromScratch = builder.assertStepScoreFromScratch;
        assertExpectedStepScore = builder.assertExpectedStepScore;
        assertShadowVariablesAreNotStaleAfterStep = builder.assertShadowVariablesAreNotStaleAfterStep;
        triggerFirstInitializedSolutionEvent = builder.triggerFirstInitializedSolutionEvent;
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

    @Override
    public boolean triggersFirstInitializedSolutionEvent() {
        return triggerFirstInitializedSolutionEvent;
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        phaseTermination.solvingStarted(solverScope);
        phaseLifecycleSupport.fireSolvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        phaseTermination.solvingEnded(solverScope);
        phaseLifecycleSupport.fireSolvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        phaseScope.startingNow();
        phaseScope.reset();
        solver.phaseStarted(phaseScope);
        phaseTermination.phaseStarted(phaseScope);
        phaseLifecycleSupport.firePhaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        solver.phaseEnded(phaseScope);
        phaseTermination.phaseEnded(phaseScope);
        phaseLifecycleSupport.firePhaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        solver.stepStarted(stepScope);
        phaseTermination.stepStarted(stepScope);
        phaseLifecycleSupport.fireStepStarted(stepScope);
    }

    protected <Score_ extends Score<Score_>> void calculateWorkingStepScore(AbstractStepScope<Solution_> stepScope,
            Object completedAction) {
        AbstractPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        Score_ score = phaseScope.calculateScore();
        stepScope.setScore(score);
        if (assertStepScoreFromScratch) {
            phaseScope.assertWorkingScoreFromScratch((Score_) stepScope.getScore(), completedAction);
        }
        if (assertShadowVariablesAreNotStaleAfterStep) {
            phaseScope.assertShadowVariablesAreNotStale((Score_) stepScope.getScore(), completedAction);
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
        solver.stepEnded(stepScope);
        collectMetrics(stepScope);
        phaseTermination.stepEnded(stepScope);
        phaseLifecycleSupport.fireStepEnded(stepScope);
    }

    private void collectMetrics(AbstractStepScope<Solution_> stepScope) {
        SolverScope<Solution_> solverScope = stepScope.getPhaseScope().getSolverScope();
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

    protected abstract static class Builder<Solution_> {

        private final int phaseIndex;
        private final boolean triggerFirstInitializedSolutionEvent;
        private final String logIndentation;
        private final Termination<Solution_> phaseTermination;

        private boolean assertStepScoreFromScratch = false;
        private boolean assertExpectedStepScore = false;
        private boolean assertShadowVariablesAreNotStaleAfterStep = false;

        protected Builder(int phaseIndex, String logIndentation, Termination<Solution_> phaseTermination) {
            this(phaseIndex, false, logIndentation, phaseTermination);
        }

        protected Builder(int phaseIndex, boolean triggerFirstInitializedSolutionEvent, String logIndentation,
                Termination<Solution_> phaseTermination) {
            this.phaseIndex = phaseIndex;
            this.triggerFirstInitializedSolutionEvent = triggerFirstInitializedSolutionEvent;
            this.logIndentation = logIndentation;
            this.phaseTermination = phaseTermination;
        }

        public void setAssertStepScoreFromScratch(boolean assertStepScoreFromScratch) {
            this.assertStepScoreFromScratch = assertStepScoreFromScratch;
        }

        public void setAssertExpectedStepScore(boolean assertExpectedStepScore) {
            this.assertExpectedStepScore = assertExpectedStepScore;
        }

        public void setAssertShadowVariablesAreNotStaleAfterStep(boolean assertShadowVariablesAreNotStaleAfterStep) {
            this.assertShadowVariablesAreNotStaleAfterStep = assertShadowVariablesAreNotStaleAfterStep;
        }

        protected abstract AbstractPhase<Solution_> build();
    }
}
