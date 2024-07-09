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

    protected final EntityPlacer<Solution_> entityPlacer;
    protected final ConstructionHeuristicDecider<Solution_> decider;
    protected final boolean isRuinPhase;

    private DefaultConstructionHeuristicPhase(Builder<Solution_> builder) {
        super(builder);
        entityPlacer = builder.entityPlacer;
        decider = builder.decider;
        isRuinPhase = builder.isRuinPhase;
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
            decider.decideNextStep(stepScope, placement, isRuinPhase);
            if (stepScope.getStep() == null) {
                if (phaseTermination.isPhaseTerminated(phaseScope) && !isRuinPhase) {
                    logger.trace("{}    Step index ({}), time spent ({}) terminated without picking a nextStep.",
                            logIndentation,
                            stepScope.getStepIndex(),
                            stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow());
                } else if (stepScope.getSelectedMoveCount() == 0L && !isRuinPhase) {
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
            if (!isRuinPhase && phaseTermination.isPhaseTerminated(phaseScope)
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
        if (!isRuinPhase) {
            solver.getBestSolutionRecaller().processWorkingSolutionDuringConstructionHeuristicsStep(stepScope);
        }
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
        if (logger.isDebugEnabled() && !isRuinPhase) {
            var timeMillisSpent = stepScope.getPhaseScope().calculateSolverTimeMillisSpentUpToNow();
            logger.debug("{}    CH step ({}), time spent ({}), score ({}), selected move count ({}),"
                    + " picked move ({}).",
                    logIndentation,
                    stepScope.getStepIndex(), timeMillisSpent,
                    stepScope.getScore(),
                    stepScope.getSelectedMoveCount(),
                    stepScope.getStepString());
        }
    }

    public void phaseEnded(ConstructionHeuristicPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        // Only update the best solution if it is not a ruin+recreate CH and the CH made any change.
        if (!isRuinPhase && !phaseScope.getStartingScore().equals(phaseScope.getBestScore())) {
            solver.getBestSolutionRecaller().updateBestSolutionAndFire(phaseScope.getSolverScope());
        }
        entityPlacer.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        if (!isRuinPhase) {
            logger.info("{}Construction Heuristic phase ({}) ended: time spent ({}), best score ({}),"
                    + " score calculation speed ({}/sec), step total ({}).",
                    logIndentation,
                    phaseIndex,
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    phaseScope.getBestScore(),
                    phaseScope.getPhaseScoreCalculationSpeed(),
                    phaseScope.getNextStepIndex());
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

    public static class Builder<Solution_> extends AbstractPhase.Builder<Solution_> {

        private final EntityPlacer<Solution_> entityPlacer;
        private final ConstructionHeuristicDecider<Solution_> decider;
        private boolean isRuinPhase = false;

        public Builder(int phaseIndex, boolean triggerFirstInitializedSolutionEvent, String logIndentation,
                Termination<Solution_> phaseTermination,
                EntityPlacer<Solution_> entityPlacer, ConstructionHeuristicDecider<Solution_> decider) {
            super(phaseIndex, triggerFirstInitializedSolutionEvent, logIndentation, phaseTermination);
            this.entityPlacer = entityPlacer;
            this.decider = decider;
        }

        public void setIsRuinPhase(boolean isRuinPhase) {
            this.isRuinPhase = isRuinPhase;
        }

        @Override
        public DefaultConstructionHeuristicPhase<Solution_> build() {
            return new DefaultConstructionHeuristicPhase<>(this);
        }
    }
}
