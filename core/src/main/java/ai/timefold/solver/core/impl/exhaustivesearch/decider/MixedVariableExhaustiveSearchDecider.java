package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

public final class MixedVariableExhaustiveSearchDecider<Solution_, Score_ extends Score<Score_>>
        extends AbstractExhaustiveSearchDecider<Solution_, Score_> {

    private final AbstractExhaustiveSearchDecider<Solution_, Score_> basicVariableDecider;
    private final AbstractExhaustiveSearchDecider<Solution_, Score_> listVariableDecider;

    private AbstractExhaustiveSearchDecider<Solution_, Score_> currentDecider = null;

    @SuppressWarnings("unchecked")
    public MixedVariableExhaustiveSearchDecider(
            AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> basicVariableDecider,
            AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> listVariableDecider) {
        super(null, null, null, null, null, null, false, null);
        this.basicVariableDecider = (AbstractExhaustiveSearchDecider<Solution_, Score_>) basicVariableDecider;
        this.listVariableDecider = (AbstractExhaustiveSearchDecider<Solution_, Score_>) listVariableDecider;
        // We start solving the basic variables,
        // and the solution will be complete only when both variable types are assigned
        // So, we will trigger the best solution events only after the basic variables are assigned
        // and the list variable is complete
        this.currentDecider = this.basicVariableDecider;
        this.currentDecider.makeSolutionAlwaysIncomplete();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope) {
        currentDecider.expandNode(stepScope);
    }

    @Override
    public boolean isSolutionComplete(ExhaustiveSearchNode expandingNode) {
        return currentDecider.isSolutionComplete(expandingNode);
    }

    @Override
    public void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope,
            boolean assertWorkingSolutionScoreFromScratch, boolean assertExpectedWorkingSolutionScore) {
        currentDecider.restoreWorkingSolution(stepScope, assertWorkingSolutionScoreFromScratch,
                assertExpectedWorkingSolutionScore);
    }

    @Override
    public boolean isEntityReinitializable(Object entity) {
        return currentDecider.isEntityReinitializable(entity);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        basicVariableDecider.solvingStarted(solverScope);
        listVariableDecider.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        basicVariableDecider.solvingEnded(solverScope);
        listVariableDecider.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        currentDecider.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        currentDecider.phaseEnded(phaseScope);
        if (currentDecider == basicVariableDecider) {
            this.currentDecider = listVariableDecider;
        }
        isFinished = this.currentDecider.isFinished;
    }

    @Override
    public void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        currentDecider.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        currentDecider.stepEnded(stepScope);
    }
}
