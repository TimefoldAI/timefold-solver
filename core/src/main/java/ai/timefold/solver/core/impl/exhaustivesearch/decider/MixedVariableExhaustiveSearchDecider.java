package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Decider for mixed models depends on the basic and lists variable deciders.
 * The optimization process begins by solving the basic variables before moving on to the list variables.
 * The process optimizes each variable separately and stops when the list variable optimization is complete.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type
 */
public final class MixedVariableExhaustiveSearchDecider<Solution_, Score_ extends Score<Score_>>
        extends AbstractExhaustiveSearchDecider<Solution_, Score_> {

    private final AbstractExhaustiveSearchDecider<Solution_, Score_> basicVariableDecider;
    private final AbstractExhaustiveSearchDecider<Solution_, Score_> listVariableDecider;

    private AbstractExhaustiveSearchDecider<Solution_, Score_> currentDecider;
    private boolean resetLastStep = false;

    @SuppressWarnings("unchecked")
    public MixedVariableExhaustiveSearchDecider(
            AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> basicVariableDecider,
            AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> listVariableDecider) {
        super(null, null, null, null, null, null, false, null);
        this.basicVariableDecider = (AbstractExhaustiveSearchDecider<Solution_, Score_>) basicVariableDecider;
        this.listVariableDecider = (AbstractExhaustiveSearchDecider<Solution_, Score_>) listVariableDecider;
        // We start solving the basic variables,
        // and the solution will be complete only when both variable types are assigned
        // So, we will trigger the best solution events with partially initialized solutions
        this.currentDecider = this.basicVariableDecider;
        this.currentDecider.enableAcceptUninitializedSolutions();
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

    @Override
    protected void fillLayerList(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        currentDecider.fillLayerList(phaseScope);
    }

    @Override
    protected void initStartNode(ExhaustiveSearchPhaseScope<Solution_> phaseScope, ExhaustiveSearchLayer layer) {
        currentDecider.initStartNode(phaseScope, layer);
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
        // We only start the phase from the current decider as it will update the phase scope
        currentDecider.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        basicVariableDecider.phaseEnded(phaseScope);
        listVariableDecider.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        currentDecider.stepStarted(stepScope);
        if (resetLastStep) {
            var phaseScope = stepScope.getPhaseScope();
            // We need to clear the queue because the starting expanding node is already being evaluated;
            // otherwise, we will solve the list variable twice.
            phaseScope.getExpandableNodeQueue().clear();
            initStartNode(phaseScope, null);
            resetLastStep = false;
        }
    }

    @Override
    public void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        currentDecider.stepEnded(stepScope);
        var isBasicDecider = currentDecider == basicVariableDecider;
        // If the search node queue is empty and we are using the basic variable decider,
        // we need to switch to the list variable decider and continue the optimization process.
        var phaseScope = stepScope.getPhaseScope();
        if (isBasicDecider && phaseScope.getExpandableNodeQueue().isEmpty()) {
            this.currentDecider = listVariableDecider;
            // The current best solution must be restored and the last step cleared
            phaseScope.getSolverScope().setWorkingSolutionFromBestSolution();
            resetLastStep = true;
            // We need to run the phase start event to reinitialize the phase scope and populate the search node queue
            phaseStarted(phaseScope);
        }
    }
}
