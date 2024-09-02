package ai.timefold.solver.core.impl.solver;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.ProblemFactChange;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.change.ProblemChangeAdapter;
import ai.timefold.solver.core.impl.solver.random.RandomFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.LongTaskTimer;
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
            BestSolutionRecaller<Solution_> bestSolutionRecaller,
            BasicPlumbingTermination<Solution_> basicPlumbingTermination, Termination<Solution_> termination,
            List<Phase<Solution_>> phaseList, SolverScope<Solution_> solverScope, String moveThreadCountDescription) {
        super(bestSolutionRecaller, termination, phaseList);
        this.environmentMode = environmentMode;
        this.randomFactory = randomFactory;
        this.basicPlumbingTermination = basicPlumbingTermination;
        this.solverScope = solverScope;
        this.moveThreadCountDescription = moveThreadCountDescription;
    }

    public EnvironmentMode getEnvironmentMode() {
        return environmentMode;
    }

    public RandomFactory getRandomFactory() {
        return randomFactory;
    }

    public InnerScoreDirectorFactory<Solution_, ?> getScoreDirectorFactory() {
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

    public long getScoreCalculationSpeed() {
        return solverScope.getScoreCalculationSpeed();
    }

    @Override
    public boolean isSolving() {
        return solving.get();
    }

    @Override
    public boolean terminateEarly() {
        boolean terminationEarlySuccessful = basicPlumbingTermination.terminateEarly();
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
    public boolean addProblemFactChange(ProblemFactChange<Solution_> problemFactChange) {
        return basicPlumbingTermination.addProblemChange(ProblemChangeAdapter.create(problemFactChange));
    }

    @Override
    public boolean addProblemFactChanges(List<ProblemFactChange<Solution_>> problemFactChangeList) {
        Objects.requireNonNull(problemFactChangeList,
                () -> "The list of problem fact changes (" + problemFactChangeList + ") cannot be null.");
        List<ProblemChangeAdapter<Solution_>> problemChangeAdapterList = problemFactChangeList.stream()
                .map(ProblemChangeAdapter::create)
                .collect(Collectors.toList());
        return basicPlumbingTermination.addProblemChanges(problemChangeAdapterList);
    }

    @Override
    public void addProblemChange(ProblemChange<Solution_> problemChange) {
        basicPlumbingTermination.addProblemChange(ProblemChangeAdapter.create(problemChange));
    }

    @Override
    public void addProblemChanges(List<ProblemChange<Solution_>> problemChangeList) {
        Objects.requireNonNull(problemChangeList,
                () -> "The list of problem changes (" + problemChangeList + ") cannot be null.");
        problemChangeList.forEach(this::addProblemChange);
    }

    @Override
    public boolean isEveryProblemChangeProcessed() {
        return basicPlumbingTermination.isEveryProblemFactChangeProcessed();
    }

    @Override
    public boolean isEveryProblemFactChangeProcessed() {
        return basicPlumbingTermination.isEveryProblemFactChangeProcessed();
    }

    public void setMonitorTagMap(Map<String, String> monitorTagMap) {
        Tags monitoringTags = Objects.requireNonNullElse(monitorTagMap, Collections.<String, String> emptyMap())
                .entrySet().stream().map(entry -> Tags.of(entry.getKey(), entry.getValue()))
                .reduce(Tags.empty(), Tags::and);
        solverScope.setMonitoringTags(monitoringTags);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public final Solution_ solve(Solution_ problem) {
        if (problem == null) {
            throw new IllegalArgumentException("The problem (" + problem + ") must not be null.");
        }

        // No tags for these metrics; they are global
        LongTaskTimer solveLengthTimer = Metrics.more().longTaskTimer(SolverMetric.SOLVE_DURATION.getMeterId());
        Counter errorCounter = Metrics.counter(SolverMetric.ERROR_COUNT.getMeterId());

        solverScope.setBestSolution(problem);
        solverScope.setSolver(this);
        outerSolvingStarted(solverScope);
        boolean restartSolver = true;
        while (restartSolver) {
            LongTaskTimer.Sample sample = solveLengthTimer.start();
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
                solverScope.getBestScore(),
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
        solverScope.setProblemSizeStatistics(
                solverScope.getSolutionDescriptor().getProblemSizeStatistics(solverScope.getWorkingSolution()));
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
            int pinIndex = listVariableDescriptor.getFirstUnpinnedIndex(entity);
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
        Class<?> factClass = fact.getClass();
        MemberAccessor planningIdAccessor = solverScope.getSolutionDescriptor().getPlanningIdAccessor(factClass);
        if (planningIdAccessor == null) { // There is no planning ID annotation.
            return;
        }
        Object id = planningIdAccessor.executeGetter(fact);
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
        logger.info("Solving ended: time spent ({}), best score ({}), score calculation speed ({}/sec), "
                + "phase total ({}), environment mode ({}), move thread count ({}).",
                solverScope.getTimeMillisSpent(),
                solverScope.getBestScore(),
                solverScope.getScoreCalculationSpeed(),
                phaseList.size(),
                environmentMode.name(),
                moveThreadCountDescription);
        // Must be kept open for doProblemFactChange
        solverScope.getScoreDirector().close();
        solving.set(false);
    }

    private boolean checkProblemFactChanges() {
        boolean restartSolver = basicPlumbingTermination.waitForRestartSolverDecision();
        if (!restartSolver) {
            return false;
        } else {
            BlockingQueue<ProblemChangeAdapter<Solution_>> problemFactChangeQueue = basicPlumbingTermination
                    .startProblemFactChangesProcessing();
            solverScope.setWorkingSolutionFromBestSolution();

            int stepIndex = 0;
            ProblemChangeAdapter<Solution_> problemChangeAdapter = problemFactChangeQueue.poll();
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
            Score<?> score = scoreDirector.calculateScore();
            basicPlumbingTermination.endProblemFactChangesProcessing();
            bestSolutionRecaller.updateBestSolutionAndFireIfInitialized(solverScope);
            logger.info("Real-time problem fact changes done: step total ({}), new best score ({}).",
                    stepIndex, score);
            return true;
        }
    }
}
