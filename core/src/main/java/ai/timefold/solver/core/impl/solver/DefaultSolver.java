package ai.timefold.solver.core.impl.solver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.ProblemFactChange;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.change.ProblemChangeAdapter;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

import org.jspecify.annotations.NonNull;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation for {@link Solver}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Solver
 * @see AbstractSolver
 */
public class DefaultSolver<Solution_> extends AbstractSolver<Solution_> {

    protected EnvironmentMode environmentMode;
    protected RandomFactory randomFactory;

    protected BasicPlumbingTermination<Solution_> basicPlumbingTermination;

    protected final AtomicBoolean solving = new AtomicBoolean(false);

    protected final SolverScope<Solution_> solverScope;

    private final String moveThreadCountDescription;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public DefaultSolver(EnvironmentMode environmentMode, RandomFactory randomFactory,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, BasicPlumbingTermination<Solution_> basicPlumbingTermination,
            UniversalTermination<Solution_> termination, List<Phase<Solution_>> phaseList,
            SolverScope<Solution_> solverScope, String moveThreadCountDescription) {
        super(bestSolutionRecaller, termination, phaseList);
        this.environmentMode = environmentMode;
        this.randomFactory = randomFactory;
        this.basicPlumbingTermination = basicPlumbingTermination;
        this.solverScope = solverScope;
        solverScope.setSolver(this);
        this.moveThreadCountDescription = moveThreadCountDescription;
    }

    public EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    public RandomFactory getRandomFactory() {
        return randomFactory;
    }

    public ScoreDirectorFactory<Solution_, ?> getScoreDirectorFactory() {
        return solverScope.getScoreDirector().getScoreDirectorFactory();
    }

    public SolverScope<Solution_> getSolverScope() {
        return solverScope;
    }

    // ************************************************************************
    // Complex getters
    // ************************************************************************

    public long getTimeMillisSpent() {
        return solverScope.getTimeMillisSpent();
    }

    public long getScoreCalculationCount() {
        return solverScope.getScoreCalculationCount();
    }

    public long getMoveEvaluationCount() {
        return solverScope.getMoveEvaluationCount();
    }

    public long getScoreCalculationSpeed() {
        return solverScope.getScoreCalculationSpeed();
    }

    public long getMoveEvaluationSpeed() {
        return solverScope.getMoveEvaluationSpeed();
    }

    @Override
    public boolean isSolving() {
        return solving.get();
    }

    @Override
    public boolean terminateEarly() {
        var terminationEarlySuccessful = basicPlumbingTermination.terminateEarly();
        if (terminationEarlySuccessful) {
            logger.info("Terminating solver early.");
        }
        return terminationEarlySuccessful;
    }

    @Override
    public boolean isTerminateEarly() {
        return basicPlumbingTermination.isTerminateEarly();
    }

    @Override
    public boolean addProblemFactChange(@NonNull ProblemFactChange<Solution_> problemFactChange) {
        return addProblemFactChanges(Collections.singletonList(problemFactChange));
    }

    @Override
    public boolean addProblemFactChanges(@NonNull List<ProblemFactChange<Solution_>> problemFactChangeList) {
        Objects.requireNonNull(problemFactChangeList,
                () -> "The list of problem fact changes (" + problemFactChangeList + ") cannot be null.");
        return basicPlumbingTermination.addProblemChanges(problemFactChangeList.stream()
                .map(ProblemChangeAdapter::create)
                .collect(Collectors.toList()));
    }

    @Override
    public void addProblemChange(@NonNull ProblemChange<Solution_> problemChange) {
        addProblemChanges(Collections.singletonList(problemChange));
    }

    @Override
    public void addProblemChanges(@NonNull List<ProblemChange<Solution_>> problemChangeList) {
        Objects.requireNonNull(problemChangeList,
                () -> "The list of problem changes (" + problemChangeList + ") cannot be null.");
        basicPlumbingTermination.addProblemChanges(problemChangeList.stream()
                .map(ProblemChangeAdapter::create)
                .toList());
    }

    @Override
    public boolean isEveryProblemChangeProcessed() {
        return basicPlumbingTermination.isEveryProblemChangeProcessed();
    }

    @Override
    public boolean isEveryProblemFactChangeProcessed() {
        return isEveryProblemChangeProcessed();
    }

    public void setMonitorTagMap(Map<String, String> monitorTagMap) {
        var monitoringTags = Objects.requireNonNullElse(monitorTagMap, Collections.<String, String> emptyMap())
                .entrySet().stream().map(entry -> Tags.of(entry.getKey(), entry.getValue()))
                .reduce(Tags.empty(), Tags::and);
        solverScope.setMonitoringTags(monitoringTags);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public final @NonNull Solution_ solve(@NonNull Solution_ problem) {
        // No tags for these metrics; they are global
        var solveLengthTimer = Metrics.more().longTaskTimer(SolverMetric.SOLVE_DURATION.getMeterId());
        var errorCounter = Metrics.counter(SolverMetric.ERROR_COUNT.getMeterId());

        solverScope.setBestSolution(Objects.requireNonNull(problem, "The problem must not be null."));
        solverScope.setSolver(this);
        outerSolvingStarted(solverScope);
        var restartSolver = true;
        while (restartSolver) {
            var sample = solveLengthTimer.start();
            try {
                // solvingStarted will call registerSolverSpecificMetrics(), since
                // the solverScope need to be fully initialized to calculate the
                // problem's scale metrics
                solvingStarted(solverScope);
                runPhases(solverScope);
                solvingEnded(solverScope);
            } catch (Exception e) {
                errorCounter.increment();
                solvingError(solverScope, e);
                throw e;
            } finally {
                sample.stop();
                unregisterSolverSpecificMetrics();
            }
            restartSolver = checkProblemFactChanges();
        }
        outerSolvingEnded(solverScope);
        return solverScope.getBestSolution();
    }

    public void outerSolvingStarted(SolverScope<Solution_> solverScope) {
        solving.set(true);
        basicPlumbingTermination.resetTerminateEarly();
        solverScope.setStartingSolverCount(0);
        solverScope.setWorkingRandom(randomFactory.createRandom());
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        assertCorrectSolutionState();
        solverScope.startingNow();
        solverScope.getScoreDirector().resetCalculationCount();
        super.solvingStarted(solverScope);
        var startingSolverCount = solverScope.getStartingSolverCount() + 1;
        solverScope.setStartingSolverCount(startingSolverCount);
        registerSolverSpecificMetrics();
        logger.info("Solving {}: time spent ({}), best score ({}), "
                + "environment mode ({}), move thread count ({}), random ({}).",
                (startingSolverCount == 1 ? "started" : "restarted"),
                solverScope.calculateTimeMillisSpentUpToNow(),
                solverScope.getBestScore().raw(),
                environmentMode.name(),
                moveThreadCountDescription,
                (randomFactory != null ? randomFactory : "not fixed"));
        if (logger.isInfoEnabled()) { // Formatting is expensive here.
            var problemSizeStatistics = solverScope.getProblemSizeStatistics();
            logger.info(
                    "Problem scale: entity count ({}), variable count ({}), approximate value count ({}), approximate problem scale ({}).",
                    problemSizeStatistics.entityCount(), problemSizeStatistics.variableCount(),
                    problemSizeStatistics.approximateValueCount(),
                    problemSizeStatistics.approximateProblemScaleAsFormattedString());
        }
    }

    private void registerSolverSpecificMetrics() {
        solverScope.getSolverMetricSet().forEach(solverMetric -> solverMetric.register(this));
    }

    private void unregisterSolverSpecificMetrics() {
        solverScope.getSolverMetricSet().forEach(solverMetric -> solverMetric.unregister(this));
    }

    private void assertCorrectSolutionState() {
        var bestSolution = solverScope.getBestSolution();
        solverScope.getSolutionDescriptor().visitAllProblemFacts(bestSolution, this::assertNonNullPlanningId);
        solverScope.getSolutionDescriptor().visitAllEntities(bestSolution, entity -> {
            assertNonNullPlanningId(entity);
            // Ensure correct state of pinning properties.
            var entityDescriptor = solverScope.getSolutionDescriptor().findEntityDescriptorOrFail(entity.getClass());
            if (!entityDescriptor.supportsPinning() || !entityDescriptor.hasAnyGenuineListVariables()) {
                return;
            }
            var listVariableDescriptor = entityDescriptor.getGenuineListVariableDescriptor();
            var pinIndex = listVariableDescriptor.getFirstUnpinnedIndex(entity);
            if (entityDescriptor.isMovable(solverScope.getScoreDirector().getWorkingSolution(), entity)) {
                if (pinIndex < 0) {
                    throw new IllegalStateException("The movable planning entity (%s) has a pin index (%s) which is negative."
                            .formatted(entity, pinIndex));
                }
                var listSize = listVariableDescriptor.getListSize(entity);
                if (pinIndex > listSize) {
                    // pinIndex == listSize is allowed, as that says the pin is at the end of the list,
                    // allowing additions to the list.
                    throw new IllegalStateException(
                            "The movable planning entity (%s) has a pin index (%s) which is greater than the list size (%s)."
                                    .formatted(entity, pinIndex, listSize));
                }
            } else {
                if (pinIndex != 0) {
                    throw new IllegalStateException("The immovable planning entity (%s) has a pin index (%s) which is not 0."
                            .formatted(entity, pinIndex));
                }
            }
        });
    }

    private void assertNonNullPlanningId(Object fact) {
        var factClass = fact.getClass();
        var planningIdAccessor = solverScope.getSolutionDescriptor().getPlanningIdAccessor(factClass);
        if (planningIdAccessor == null) { // There is no planning ID annotation.
            return;
        }
        var id = planningIdAccessor.executeGetter(fact);
        if (id == null) { // Fail fast as planning ID is null.
            throw new IllegalStateException("The planningId (" + id + ") of the member (" + planningIdAccessor
                    + ") of the class (" + factClass + ") on object (" + fact + ") must not be null.\n"
                    + "Maybe initialize the planningId of the class (" + planningIdAccessor.getDeclaringClass()
                    + ") instance (" + fact + ") before solving.\n" +
                    "Maybe remove the @" + PlanningId.class.getSimpleName() + " annotation.");
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        solverScope.endingNow();
    }

    public void outerSolvingEnded(SolverScope<Solution_> solverScope) {
        logger.info("Solving ended: time spent ({}), best score ({}), move evaluation speed ({}/sec), "
                + "phase total ({}), environment mode ({}), move thread count ({}).",
                solverScope.getTimeMillisSpent(),
                solverScope.getBestScore().raw(),
                solverScope.getMoveEvaluationSpeed(),
                phaseList.size(),
                environmentMode.name(),
                moveThreadCountDescription);
        // Must be kept open for doProblemFactChange
        solverScope.getScoreDirector().close();
        solving.set(false);
    }

    private boolean checkProblemFactChanges() {
        var restartSolver = basicPlumbingTermination.waitForRestartSolverDecision();
        if (!restartSolver) {
            return false;
        } else {
            var problemFactChangeQueue = basicPlumbingTermination
                    .startProblemChangesProcessing();
            solverScope.setWorkingSolutionFromBestSolution();

            var stepIndex = 0;
            var problemChangeAdapter = problemFactChangeQueue.poll();
            while (problemChangeAdapter != null) {
                problemChangeAdapter.doProblemChange(solverScope);
                logger.debug("    Real-time problem change applied; step index ({}).", stepIndex);
                stepIndex++;
                problemChangeAdapter = problemFactChangeQueue.poll();
            }
            // All PFCs are processed, fail fast if any of the new facts have null planning IDs.
            InnerScoreDirector<Solution_, ?> scoreDirector = solverScope.getScoreDirector();
            assertCorrectSolutionState();
            // Everything is fine, proceed.
            var score = scoreDirector.calculateScore();
            basicPlumbingTermination.endProblemChangesProcessing();
            bestSolutionRecaller.updateBestSolutionAndFireIfInitialized(solverScope);
            logger.info("Real-time problem fact changes done: step total ({}), new best score ({}).",
                    stepIndex, score);
            return true;
        }
    }
}
