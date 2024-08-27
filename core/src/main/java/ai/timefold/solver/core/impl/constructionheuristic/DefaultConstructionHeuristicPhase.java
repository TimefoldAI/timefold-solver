package ai.timefold.solver.core.impl.constructionheuristic;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.placer.EntityPlacer;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicStepScope;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

/**
 * Default implementation of {@link ConstructionHeuristicPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class DefaultConstructionHeuristicPhase<Solution_> extends AbstractPhase<Solution_>
        implements ConstructionHeuristicPhase<Solution_> {

    protected final ConstructionHeuristicDecider<Solution_> decider;
    protected final EntityPlacer<Solution_> entityPlacer;

    protected DefaultConstructionHeuristicPhase(DefaultConstructionHeuristicPhaseBuilder<Solution_> builder) {
        super(builder);
        decider = builder.decider;
        entityPlacer = builder.getEntityPlacer();
    }

    public EntityPlacer<Solution_> getEntityPlacer() {
        return entityPlacer;
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
        var phaseScope = new ConstructionHeuristicPhaseScope<>(solverScope, phaseIndex);
        phaseStarted(phaseScope);

        var solutionDescriptor = solverScope.getSolutionDescriptor();
        var listVariableDescriptor = solutionDescriptor.getListVariableDescriptor();
        var hasListVariable = listVariableDescriptor != null;
        var maxStepCount = -1;
        if (hasListVariable) {
            // In case of list variable with support for unassigned values, the placer will iterate indefinitely.
            // (When it exhausts all values, it will start over from the beginning.)
            // To prevent that, we need to limit the number of steps to the number of unassigned values.
            var workingSolution = phaseScope.getWorkingSolution();
            maxStepCount = listVariableDescriptor.countUnassigned(workingSolution);
        }

        for (var placement : entityPlacer) {
            var stepScope = new ConstructionHeuristicStepScope<>(phaseScope);
            stepStarted(stepScope);
            decider.decideNextStep(stepScope, placement);
            if (stepScope.getStep() == null) {
                if (phaseTermination.isPhaseTerminated(phaseScope)
                        && decider.isLoggingEnabled()
                        && logger.isTraceEnabled()) {
                    logger.trace("{}    Step index ({}), time spent ({}) terminated without picking a nextStep.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else if (stepScope.getSelectedMoveCount() == 0L
                        && decider.isLoggingEnabled()
                        && logger.isWarnEnabled()) {
                    logger.warn("{}    No doable selected move at step index ({}), time spent ({})."
                            + " Terminating phase early.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else {
                    throw new IllegalStateException("The step index (" + stepScope.getStepIndex()
                            + ") has selected move count (" + stepScope.getSelectedMoveCount()
                            + ") but failed to pick a nextStep (" + stepScope.getStep() + ").");
                }
                // Although stepStarted has been called, stepEnded is not called for this step
                break;
            }
            doStep(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
            if (phaseTermination.isPhaseTerminated(phaseScope)
                    || (hasListVariable && stepScope.getStepIndex() >= maxStepCount)) {
                break;
            }
        }
        phaseEnded(phaseScope);
    }

    private void doStep(ConstructionHeuristicStepScope<Solution_> stepScope) {
        var step = stepScope.getStep();
        step.doMoveOnly(stepScope.getScoreDirector());
        predictWorkingStepScore(stepScope, step);
        processWorkingSolutionDuringStep(stepScope);
    }

    protected void processWorkingSolutionDuringStep(ConstructionHeuristicStepScope<Solution_> stepScope) {
        solver.getBestSolutionRecaller().processWorkingSolutionDuringConstructionHeuristicsStep(stepScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        entityPlacer.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    public void phaseStarted(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        entityPlacer.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
    }

    public void stepStarted(ConstructionHeuristicStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        entityPlacer.stepStarted(stepScope);
        decider.stepStarted(stepScope);
    }

    public void stepEnded(ConstructionHeuristicStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        entityPlacer.stepEnded(stepScope);
        decider.stepEnded(stepScope);
        if (decider.isLoggingEnabled() && logger.isDebugEnabled()) {
            var timeMillisSpent = stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow();
            logger.debug("{}    CH step ({}), time spent ({}), score ({}), selected move count ({}), picked move ({}).",
                    logIndentation,
                    stepScope.getStepIndex(), timeMillisSpent,
                    stepScope.getScore(),
                    stepScope.getSelectedMoveCount(),
                    stepScope.getStepString());
        }
    }

    public void phaseEnded(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        updateBestSolutionAndFire(phaseScope);
        entityPlacer.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        if (decider.isLoggingEnabled() && logger.isInfoEnabled()) {
            logger.info(
                    "{}Construction Heuristic phase ({}) ended: time spent ({}), best score ({}), score calculation speed ({}/sec), step total ({}).",
                    logIndentation,
                    phaseIndex,
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    phaseScope.getBestScore(),
                    phaseScope.getPhaseScoreCalculationSpeed(),
                    phaseScope.getNextStepIndex());
        }
    }

    protected void updateBestSolutionAndFire(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        // Only update the best solution if the CH made any change.
        if (!phaseScope.getStartingScore().equals(phaseScope.getBestScore())) {
            solver.getBestSolutionRecaller().updateBestSolutionAndFire(phaseScope.getSolverScope());
        }
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        entityPlacer.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

    @Override
    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        super.solvingError(solverScope, exception);
        decider.solvingError(solverScope, exception);
    }

    public static class DefaultConstructionHeuristicPhaseBuilder<Solution_>
            extends AbstractPhase.Builder<Solution_> {

        private final EntityPlacer<Solution_> entityPlacer;
        private final ConstructionHeuristicDecider<Solution_> decider;

        public DefaultConstructionHeuristicPhaseBuilder(int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
                String logIndentation, Termination<Solution_> phaseTermination, EntityPlacer<Solution_> entityPlacer,
                ConstructionHeuristicDecider<Solution_> decider) {
            super(phaseIndex, triggerFirstInitializedSolutionEvent, logIndentation, phaseTermination);
            this.entityPlacer = entityPlacer;
            this.decider = decider;
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
