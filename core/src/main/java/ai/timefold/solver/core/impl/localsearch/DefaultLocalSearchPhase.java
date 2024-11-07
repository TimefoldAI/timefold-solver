package ai.timefold.solver.core.impl.localsearch;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.event.LocalSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation of {@link LocalSearchPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class DefaultLocalSearchPhase<Solution_> extends AbstractPhase<Solution_> implements LocalSearchPhase<Solution_>,
        LocalSearchPhaseLifecycleListener<Solution_> {

    protected final LocalSearchDecider<Solution_> decider;
    protected final AtomicLong acceptedMoveCountPerStep = new AtomicLong(0);
    protected final AtomicLong selectedMoveCountPerStep = new AtomicLong(0);
    protected final Map<Tags, AtomicLong> constraintMatchTotalTagsToStepCount = new ConcurrentHashMap<>();
    protected final Map<Tags, AtomicLong> constraintMatchTotalTagsToBestCount = new ConcurrentHashMap<>();
    protected final Map<Tags, List<AtomicReference<Number>>> constraintMatchTotalStepScoreMap = new ConcurrentHashMap<>();
    protected final Map<Tags, List<AtomicReference<Number>>> constraintMatchTotalBestScoreMap = new ConcurrentHashMap<>();

    private DefaultLocalSearchPhase(Builder<Solution_> builder) {
        super(builder);
        decider = builder.decider;
    }

    @Override
    public String getPhaseTypeString() {
        return "Local Search";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        LocalSearchPhaseScope<Solution_> phaseScope = new LocalSearchPhaseScope<>(solverScope, phaseIndex);
        phaseStarted(phaseScope);

        if (solverScope.isMetricEnabled(SolverMetric.MOVE_COUNT_PER_STEP)) {
            Metrics.gauge(SolverMetric.MOVE_COUNT_PER_STEP.getMeterId() + ".accepted",
                    solverScope.getMonitoringTags(), acceptedMoveCountPerStep);
            Metrics.gauge(SolverMetric.MOVE_COUNT_PER_STEP.getMeterId() + ".selected",
                    solverScope.getMonitoringTags(), selectedMoveCountPerStep);
        }

        while (!phaseTermination.isPhaseTerminated(phaseScope)) {
            LocalSearchStepScope<Solution_> stepScope = new LocalSearchStepScope<>(phaseScope);
            stepScope.setTimeGradient(phaseTermination.calculatePhaseTimeGradient(phaseScope));
            stepStarted(stepScope);
            decider.decideNextStep(stepScope);
            if (stepScope.getStep() == null) {
                if (phaseTermination.isPhaseTerminated(phaseScope)) {
                    logger.trace("{}    Step index ({}), time spent ({}) terminated without picking a nextStep.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else if (stepScope.getSelectedMoveCount() == 0L) {
                    logger.warn("{}    No doable selected move at step index ({}), time spent ({})."
                            + " Terminating phase early.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else {
                    throw new IllegalStateException("The step index (" + stepScope.getStepIndex()
                            + ") has accepted/selected move count (" + stepScope.getAcceptedMoveCount() + "/"
                            + stepScope.getSelectedMoveCount()
                            + ") but failed to pick a nextStep (" + stepScope.getStep() + ").");
                }
                // Although stepStarted has been called, stepEnded is not called for this step
                break;
            }
            doStep(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        phaseEnded(phaseScope);
    }

    protected void doStep(LocalSearchStepScope<Solution_> stepScope) {
        var step = stepScope.getStep();
        step.execute(stepScope.getMoveDirector());
        predictWorkingStepScore(stepScope, step);
        solver.getBestSolutionRecaller().processWorkingSolutionDuringStep(stepScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
        // TODO maybe this restriction should be lifted to allow LocalSearch to initialize a solution too?
        assertWorkingSolutionInitialized(phaseScope);
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        decider.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        decider.stepEnded(stepScope);
        collectMetrics(stepScope);
        LocalSearchPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        if (logger.isDebugEnabled()) {
            logger.debug("{}    LS step ({}), time spent ({}), score ({}), {} best score ({})," +
                    " accepted/selected move count ({}/{}), picked move ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getScore(),
                    (stepScope.getBestScoreImproved() ? "new" : "   "), phaseScope.getBestScore(),
                    stepScope.getAcceptedMoveCount(),
                    stepScope.getSelectedMoveCount(),
                    stepScope.getStepString());
        }
    }

    private void collectMetrics(LocalSearchStepScope<Solution_> stepScope) {
        LocalSearchPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        SolverScope<Solution_> solverScope = phaseScope.getSolverScope();
        if (solverScope.isMetricEnabled(SolverMetric.MOVE_COUNT_PER_STEP)) {
            acceptedMoveCountPerStep.set(stepScope.getAcceptedMoveCount());
            selectedMoveCountPerStep.set(stepScope.getSelectedMoveCount());
        }
        if (solverScope.isMetricEnabled(SolverMetric.CONSTRAINT_MATCH_TOTAL_STEP_SCORE)
                || solverScope.isMetricEnabled(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE)) {
            InnerScoreDirector<Solution_, ?> scoreDirector = stepScope.getScoreDirector();
            ScoreDefinition<?> scoreDefinition = solverScope.getScoreDefinition();
            if (scoreDirector.getConstraintMatchPolicy().isEnabled()) {
                for (ConstraintMatchTotal<?> constraintMatchTotal : scoreDirector.getConstraintMatchTotalMap()
                        .values()) {
                    Tags tags = solverScope.getMonitoringTags().and(
                            "constraint.package", constraintMatchTotal.getConstraintRef().packageName(),
                            "constraint.name", constraintMatchTotal.getConstraintRef().constraintName());
                    collectConstraintMatchTotalMetrics(SolverMetric.CONSTRAINT_MATCH_TOTAL_BEST_SCORE, tags,
                            constraintMatchTotalTagsToBestCount,
                            constraintMatchTotalBestScoreMap, constraintMatchTotal, scoreDefinition, solverScope);
                    collectConstraintMatchTotalMetrics(SolverMetric.CONSTRAINT_MATCH_TOTAL_STEP_SCORE, tags,
                            constraintMatchTotalTagsToStepCount,
                            constraintMatchTotalStepScoreMap, constraintMatchTotal, scoreDefinition, solverScope);
                }
            }
        }
    }

    private void collectConstraintMatchTotalMetrics(SolverMetric metric, Tags tags, Map<Tags, AtomicLong> countMap,
            Map<Tags, List<AtomicReference<Number>>> scoreMap, ConstraintMatchTotal<?> constraintMatchTotal,
            ScoreDefinition<?> scoreDefinition, SolverScope<Solution_> solverScope) {
        if (solverScope.isMetricEnabled(metric)) {
            if (countMap.containsKey(tags)) {
                countMap.get(tags).set(constraintMatchTotal.getConstraintMatchCount());
            } else {
                AtomicLong count = new AtomicLong(constraintMatchTotal.getConstraintMatchCount());
                countMap.put(tags, count);
                Metrics.gauge(metric.getMeterId() + ".count",
                        tags, count);
            }
            SolverMetric.registerScoreMetrics(metric,
                    tags, scoreDefinition, scoreMap, constraintMatchTotal.getScore());
        }
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        logger.info("{}Local Search phase ({}) ended: time spent ({}), best score ({}),"
                + " move evaluation speed ({}/sec), step total ({}).",
                logIndentation,
                phaseIndex,
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore(),
                phaseScope.getPhaseMoveEvaluationSpeed(),
                phaseScope.getNextStepIndex());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

    @Override
    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        super.solvingError(solverScope, exception);
        decider.solvingError(solverScope, exception);
    }

    public static class Builder<Solution_> extends AbstractPhase.Builder<Solution_> {

        private final LocalSearchDecider<Solution_> decider;

        public Builder(int phaseIndex, String logIndentation, Termination<Solution_> phaseTermination,
                LocalSearchDecider<Solution_> decider) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.decider = decider;
        }

        @Override
        public DefaultLocalSearchPhase<Solution_> build() {
            return new DefaultLocalSearchPhase<>(this);
        }
    }
}
