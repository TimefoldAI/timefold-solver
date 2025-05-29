package ai.timefold.solver.core.impl.constructionheuristic;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.move.PlacerBasedMoveRepository;
import ai.timefold.solver.core.impl.phase.AbstractPossiblyInitializingPhase;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

import org.jspecify.annotations.NullMarked;
import org.slf4j.event.Level;

/**
 * Default implementation of {@link ConstructionHeuristicPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
@NullMarked
public class DefaultConstructionHeuristicPhase<Solution_>
        extends AbstractPossiblyInitializingPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    protected final ConstructionHeuristicDecider<Solution_> decider;
    protected final PlacerBasedMoveRepository<Solution_> moveRepository;
    private TerminationStatus terminationStatus = TerminationStatus.NOT_TERMINATED;

    protected DefaultConstructionHeuristicPhase(DefaultConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
        this.decider = builder.decider;
        this.moveRepository = new PlacerBasedMoveRepository<>(builder.getEntityPlacer());
    }

    public EntityPlacer<Solution_> getEntityPlacer() {
        return moveRepository.getPlacer();
    }

    @Override
    public TerminationStatus getTerminationStatus() {
        return terminationStatus;
    }

    @Override
    public String getPhaseTypeString() {
        return "Construction Heuristics";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************
    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var phaseScope = buildPhaseScope(solverScope, phaseIndex);
        phaseStarted(phaseScope);

        var solutionDescriptor = solverScope.getSolutionDescriptor();
        var hasListVariable = moveRepository.hasListVariable();
        var maxStepCount = -1;
        if (hasListVariable) {
            // In case of list variable with support for unassigned values, the placer will iterate indefinitely.
            // (When it exhausts all values, it will start over from the beginning.)
            // To prevent that, we need to limit the number of steps to the number of unassigned values.
            var workingSolution = phaseScope.getWorkingSolution();
            maxStepCount = solutionDescriptor.getListVariableDescriptor().countUnassigned(workingSolution);
        }

        TerminationStatus earlyTerminationStatus = null;
        while (moveRepository.hasNext()) {
            var stepScope = new ConstructionHeuristicStepScope<>(phaseScope);
            stepStarted(stepScope);
            decider.decideNextStep(stepScope, moveRepository.iterator());
            if (stepScope.getStep() == null) {
                if (phaseTermination.isPhaseTerminated(phaseScope)) {
                    var logLevel = Level.TRACE;
                    if (decider.isLoggingEnabled() && logger.isEnabledForLevel(logLevel)) {
                        logger.atLevel(logLevel).log(
                                "{}    Step index ({}), time spent ({}) terminated without picking a nextStep.",
                                logIndentation, stepScope.getStepIndex(),
                                stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                    }
                } else if (stepScope.getSelectedMoveCount() == 0L) {
                    var logLevel = Level.WARN;
                    if (decider.isLoggingEnabled() && logger.isEnabledForLevel(logLevel)) {
                        logger.atLevel(logLevel).log(
                                "{}    No doable selected move at step index ({}), time spent ({}). Terminating phase early.",
                                logIndentation, stepScope.getStepIndex(),
                                stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                    }
                } else {
                    throw new IllegalStateException("The step index (" + stepScope.getStepIndex()
                            + ") has selected move count (" + stepScope.getSelectedMoveCount()
                            + ") but failed to pick a nextStep (" + stepScope.getStep() + ").");
                }
                // Although stepStarted has been called, stepEnded is not called for this step.
                earlyTerminationStatus = TerminationStatus.early(phaseScope.getNextStepIndex());
                break;
            }
            doStep(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
            if (hasListVariable && stepScope.getStepIndex() >= maxStepCount) {
                earlyTerminationStatus = TerminationStatus.regular(phaseScope.getNextStepIndex());
                break;
            } else if (phaseTermination.isPhaseTerminated(phaseScope)) {
                earlyTerminationStatus = TerminationStatus.early(phaseScope.getNextStepIndex());
                break;
            }
        }
        // We only store the termination status, which is exposed to the outside, when the phase has ended.
        terminationStatus = translateEarlyTermination(phaseScope, earlyTerminationStatus, moveRepository.hasNext());
        phaseEnded(phaseScope);
    }

    protected ConstructionHeuristicPhaseScope<Solution_> buildPhaseScope(SolverScope<Solution_> solverScope, int phaseIndex) {
        return new ConstructionHeuristicPhaseScope<>(solverScope, phaseIndex);
    }

    protected void doStep(ConstructionHeuristicStepScope<Solution_> stepScope) {
        var step = stepScope.getStep();
        stepScope.getScoreDirector().executeMove(step);
        predictWorkingStepScore(stepScope, step);
        if (!isNested()) {
            processWorkingSolutionDuringStep(stepScope);
        }
    }

    private void processWorkingSolutionDuringStep(ConstructionHeuristicStepScope<Solution_> stepScope) {
        var solver = stepScope.getPhaseScope().getSolverScope().getSolver();
        solver.getBestSolutionRecaller().processWorkingSolutionDuringConstructionHeuristicsStep(stepScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        moveRepository.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    public void phaseStarted(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        terminationStatus = TerminationStatus.NOT_TERMINATED;
        moveRepository.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
    }

    public void stepStarted(ConstructionHeuristicStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        moveRepository.stepStarted(stepScope);
        decider.stepStarted(stepScope);
    }

    public void stepEnded(ConstructionHeuristicStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        moveRepository.stepEnded(stepScope);
        decider.stepEnded(stepScope);
        if (decider.isLoggingEnabled() && logger.isDebugEnabled()) {
            var timeMillisSpent = stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow();
            logger.debug("{}    CH step ({}), time spent ({}), score ({}), selected move count ({}), picked move ({}).",
                    logIndentation,
                    stepScope.getStepIndex(), timeMillisSpent,
                    stepScope.getScore().raw(),
                    stepScope.getSelectedMoveCount(),
                    stepScope.getStepString());
        }
    }

    public void phaseEnded(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        ensureCorrectTermination(phaseScope, logger);
        updateBestSolutionAndFire(phaseScope);
        moveRepository.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        if (decider.isLoggingEnabled() && logger.isInfoEnabled()) {
            logger.info(
                    "{}Construction Heuristic phase ({}) ended: time spent ({}), best score ({}), move evaluation speed ({}/sec), step total ({}).",
                    logIndentation,
                    phaseIndex,
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    phaseScope.getBestScore().raw(),
                    phaseScope.getPhaseMoveEvaluationSpeed(),
                    phaseScope.getNextStepIndex());
        }
    }

    private void updateBestSolutionAndFire(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        if (!isNested() && !phaseScope.getStartingScore().equals(phaseScope.getBestScore())) {
            // Only update the best solution if the CH made any change; nested phases don't update the best solution.
            var solver = phaseScope.getSolverScope().getSolver();
            solver.getBestSolutionRecaller().updateBestSolutionAndFire(phaseScope.getSolverScope());
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        moveRepository.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

    @Override
    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        super.solvingError(solverScope, exception);
        decider.solvingError(solverScope, exception);
    }

    public static class DefaultConstructionHeuristicPhaseBuilder<Solution_>
            extends AbstractPossiblyInitializingPhaseBuilder<Solution_> {

        private final EntityPlacer<Solution_> entityPlacer;
        private final ConstructionHeuristicDecider<Solution_> decider;

        public DefaultConstructionHeuristicPhaseBuilder(int phaseIndex, boolean lastInitializingPhase, String logIndentation,
                PhaseTermination<Solution_> phaseTermination, EntityPlacer<Solution_> entityPlacer,
                ConstructionHeuristicDecider<Solution_> decider) {
            super(phaseIndex, lastInitializingPhase, logIndentation, phaseTermination);
            this.entityPlacer = entityPlacer;
            this.decider = decider;
        }

        @Override
        public DefaultConstructionHeuristicPhaseBuilder<Solution_> enableAssertions(EnvironmentMode environmentMode) {
            super.enableAssertions(environmentMode);
            return this;
        }

        public EntityPlacer<Solution_> getEntityPlacer() {
            return entityPlacer;
        }

        @Override
        public DefaultConstructionHeuristicPhase<Solution_> build() {
            return new DefaultConstructionHeuristicPhase<>(this);
        }
    }

}
