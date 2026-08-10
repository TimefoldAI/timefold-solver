package ai.timefold.solver.core.impl.solver;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.monitoring.SolverMetric;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.score.director.DelegateScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.BasicPlumbingTermination;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

import org.jspecify.annotations.NullMarked;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation for {@link Solver}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @see Solver
 * @see AbstractSolver
 */
@NullMarked
public class DefaultSolver<Solution_> extends AbstractSolver<Solution_> {

    private final DelegateScoreDirectorFactory<Solution_, ?> delegateScoreDirectorFactory;
    private final Supplier<RandomSource> randomFactory;
    private final BasicPlumbingTermination<Solution_> basicPlumbingTermination;
    private final AtomicBoolean solving = new AtomicBoolean(false);
    private final SolverScope<Solution_> solverScope;
    private final String moveThreadCountDescription;
    private final SolverContext<Solution_, ?> defaultSolverContext;
    private SolverContext<Solution_, ?> currentContext;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public DefaultSolver(EnvironmentMode environmentMode,
            DelegateScoreDirectorFactory<Solution_, ?> delegateScoreDirectorFactory,
            Supplier<RandomSource> randomFactory, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            BasicPlumbingTermination<Solution_> basicPlumbingTermination, UniversalTermination<Solution_> termination,
            List<Phase<Solution_>> phaseList, SolverScope<Solution_> solverScope, String moveThreadCountDescription) {
        super(bestSolutionRecaller, termination, phaseList);
        this.delegateScoreDirectorFactory = delegateScoreDirectorFactory;
        this.randomFactory = randomFactory;
        this.basicPlumbingTermination = basicPlumbingTermination;
        this.solverScope = solverScope;
        solverScope.setSolver(this);
        this.moveThreadCountDescription = moveThreadCountDescription;
        this.defaultSolverContext = SolverContext.of(environmentMode, solverScope);
        this.currentContext = defaultSolverContext;
    }

    public RandomSource getRandomSource() {
        return randomFactory.get();
    }

    @SuppressWarnings({ "unchecked", "resource" })
    public <Score_ extends Score<Score_>> ScoreDirectorFactory<Solution_, Score_> getScoreDirectorFactory() {
        InnerScoreDirector<Solution_, Score_> scoreDirector =
                (InnerScoreDirector<Solution_, Score_>) defaultSolverContext.scoreDirector();
        return scoreDirector.getScoreDirectorFactory();
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
            LOGGER.info("Terminating solver early.");
        }
        return terminationEarlySuccessful;
    }

    @Override
    public boolean isTerminateEarly() {
        return basicPlumbingTermination.isTerminateEarly();
    }

    @Override
    public void addProblemChange(ProblemChange<Solution_> problemChange) {
        addProblemChanges(Collections.singletonList(problemChange));
    }

    @Override
    public void addProblemChanges(List<ProblemChange<Solution_>> problemChangeList) {
        Objects.requireNonNull(problemChangeList,
                () -> "The list of problem changes (%s) cannot be null."
                        .formatted(problemChangeList));
        basicPlumbingTermination.addProblemChanges(problemChangeList);
    }

    @Override
    public boolean isEveryProblemChangeProcessed() {
        return basicPlumbingTermination.isEveryProblemChangeProcessed();
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
    public final Solution_ solve(Solution_ problem) {
        // No tags for these metrics; they are global
        var solveLengthTimer = Metrics.more().longTaskTimer(SolverMetric.SOLVE_DURATION.getMeterId());
        var errorCounter = Metrics.counter(SolverMetric.ERROR_COUNT.getMeterId());

        solverScope.setInitialSolution(Objects.requireNonNull(problem, "The problem must not be null."));
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
            restartSolver = checkProblemChanges();
        }
        outerSolvingEnded(solverScope);
        return solverScope.getBestSolution();
    }

    protected void runPhases(SolverScope<Solution_> solverScope) {
        if (!solverScope.getSolutionDescriptor().hasMovableEntities(solverScope.getScoreDirector())) {
            logger.info("Skipped all phases ({}): out of {} planning entities, none are movable (non-pinned).",
                    phaseList.size(), solverScope.getWorkingEntityCount());
            return;
        }
        Iterator<Phase<Solution_>> it = phaseList.iterator();
        while (!globalTermination.isSolverTerminated(solverScope) && it.hasNext()) {
            Phase<Solution_> phase = it.next();
            preparePhase(phase);
            phase.solve(solverScope);
            // If there is a next phase, it starts from the best solution, which might differ from the working solution.
            // If there isn't, no need to planning clone the best solution to the working solution.
            if (it.hasNext()) {
                solverScope.setWorkingSolutionFromBestSolution();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void preparePhase(Phase<Solution_> phase) {
        // The environment modes match, and there is no need for any changes.
        if (phase.getEnvironmentMode() == currentContext.environmentMode()) {
            return;
        }
        // The phase environment mode matches default, so we will restore it.
        if (phase.getEnvironmentMode() == defaultSolverContext.environmentMode()) {
            // Release the current context
            currentContext.release();
            // Update and load the default context
            currentContext = defaultSolverContext;
            currentContext.load(solverScope);
            return;
        }
        // Since the current logic does not cache any solver context other than the default,
        // we need to create a new solver context
        // because the required environment mode differs from both the current and the default modes.
        ScoreDirectorFactory newScoreDirectorFactory = delegateScoreDirectorFactory
                .buildScoreDirectorFactory(phase.getEnvironmentMode(), solverScope.getSolutionDescriptor());
        var newScoreDirector = delegateScoreDirectorFactory.createScoreDirector(newScoreDirectorFactory);
        var newSolverContext = new SolverContext<>(phase.getEnvironmentMode(), newScoreDirector,
                new DefaultProblemChangeDirector<>(newScoreDirector), currentContext.bestSolutionRecaller);
        // Release the current context
        if (currentContext != defaultSolverContext) {
            currentContext.release();
        }
        // Update and load the new context
        currentContext = newSolverContext;
        currentContext.load(solverScope);
    }

    public void outerSolvingStarted(SolverScope<Solution_> solverScope) {
        solving.set(true);
        basicPlumbingTermination.resetTerminateEarly();
        solverScope.setStartingSolverCount(0);
        solverScope.setWorkingRandom(randomFactory.get());
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

        // Update the best solution, since problem's shadows and score were updated
        bestSolutionRecaller.updateBestSolutionAndFireIfInitialized(solverScope,
                EventProducerId.solvingStarted());

        LOGGER.info("Solving {}: time spent ({}), best score ({}), "
                + "environment mode ({}), move thread count ({}), random ({}).",
                (startingSolverCount == 1 ? "started" : "restarted"),
                solverScope.calculateTimeMillisSpentUpToNow(),
                solverScope.getBestScore().raw(),
                defaultSolverContext.environmentMode().name(),
                moveThreadCountDescription,
                randomFactory);
        if (LOGGER.isInfoEnabled()) { // Formatting is expensive here.
            var problemSizeStatistics = solverScope.getProblemSizeStatistics();
            LOGGER.info(
                    "Problem scale: genuine entity count ({}), genuine variable count ({}), approximate value count ({}), approximate problem scale ({}).",
                    problemSizeStatistics.entityCount(), problemSizeStatistics.variableCount(),
                    problemSizeStatistics.approximateValueCount(),
                    problemSizeStatistics.approximateProblemScaleAsFormattedString());
            if (LOGGER.isDebugEnabled()) {
                var genuineEntityClassCountEntries = problemSizeStatistics.genuineEntityClassToEntityCount().entrySet();
                var solutionDescriptor = solverScope.getSolutionDescriptor();
                for (var genuineEntityCountEntry : genuineEntityClassCountEntries) {
                    var geninueEntityClass = genuineEntityCountEntry.getKey();
                    var entityDescriptor = solutionDescriptor.findEntityDescriptorOrFail(geninueEntityClass);
                    LOGGER.debug("    Entity ({}) count: {}",
                            geninueEntityClass.getCanonicalName(),
                            genuineEntityCountEntry.getValue());
                    for (var geninueVariableEntry : problemSizeStatistics
                            .genuineEntityClassToVariableToValueCount()
                            .get(geninueEntityClass).entrySet()) {
                        var genuineVariable = geninueVariableEntry.getKey();
                        var variableDescriptor = entityDescriptor.getGenuineVariableDescriptor(genuineVariable);
                        LOGGER.debug("        {} ({}) estimated value ({}) count: {}",
                                (variableDescriptor instanceof ListVariableDescriptor) ? "List variable" : "Variable",
                                genuineVariable,
                                (variableDescriptor instanceof ListVariableDescriptor<?> listVariableDescriptor)
                                        ? listVariableDescriptor.getElementType().getCanonicalName()
                                        : variableDescriptor.getVariablePropertyType().getCanonicalName(),
                                geninueVariableEntry.getValue());
                    }
                }
            }
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
            if (!entityDescriptor.supportsPinning() || !entityDescriptor.hasAnyListVariables()) {
                return;
            }
            var listVariableDescriptor = entityDescriptor.getListVariableDescriptor();
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
                defaultSolverContext.environmentMode().name(),
                moveThreadCountDescription);
        // Must be kept open for doProblemFactChange
        solverScope.getScoreDirector().close();
        solving.set(false);
    }

    private boolean checkProblemChanges() {
        var restartSolver = basicPlumbingTermination.waitForRestartSolverDecision();
        if (!restartSolver) {
            return false;
        } else {
            var problemChangeQueue = basicPlumbingTermination
                    .startProblemChangesProcessing();
            solverScope.setWorkingSolutionFromBestSolution();

            var stepIndex = 0;
            var problemChange = problemChangeQueue.poll();
            while (problemChange != null) {
                problemChange.doChange(solverScope.getWorkingSolution(), solverScope.getProblemChangeDirector());
                solverScope.getScoreDirector().updateShadowVariables();
                logger.debug("    Real-time problem change applied; step index ({}).", stepIndex);
                stepIndex++;
                problemChange = problemChangeQueue.poll();
            }
            // All PFCs are processed, fail fast if any of the new facts have null planning IDs.
            InnerScoreDirector<Solution_, ?> scoreDirector = solverScope.getScoreDirector();
            assertCorrectSolutionState();
            // Everything is fine, proceed.
            var score = scoreDirector.calculateScore();
            basicPlumbingTermination.endProblemChangesProcessing();
            bestSolutionRecaller.updateBestSolutionAndFireIfInitialized(solverScope,
                    EventProducerId.problemChange());
            logger.info("Real-time problem fact changes done: step total ({}), new best score ({}).",
                    stepIndex, score);
            return true;
        }
    }

    private record SolverContext<Solution_, Score_ extends Score<Score_>>(EnvironmentMode environmentMode,
            InnerScoreDirector<Solution_, Score_> scoreDirector, DefaultProblemChangeDirector<Solution_> problemChangeDirector,
            BestSolutionRecaller<Solution_> bestSolutionRecaller) {

        static <Solution_, Score_ extends Score<Score_>> SolverContext<Solution_, Score_>
                of(EnvironmentMode environmentMode, SolverScope<Solution_> solverScope) {
            return new SolverContext<>(environmentMode, solverScope.<Score_> getScoreDirector(),
                    solverScope.getProblemChangeDirector(), solverScope.getSolver().getBestSolutionRecaller());
        }

        void load(SolverScope<Solution_> solverScope) {
            solverScope.setScoreDirector(scoreDirector);
            solverScope.setProblemChangeDirector(problemChangeDirector);
            solverScope.setWorkingSolutionFromBestSolution();
            bestSolutionRecaller.enableAssertions(environmentMode);
        }

        void release() {
            scoreDirector.close();
        }
    }
}
