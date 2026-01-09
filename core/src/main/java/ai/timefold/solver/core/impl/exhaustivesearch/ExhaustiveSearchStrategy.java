package ai.timefold.solver.core.impl.exhaustivesearch;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.exhaustivesearch.event.ExhaustiveSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;

/**
 * Base contract that allows the exhaustive method to apply different strategies according to the variable type.
 *
 * @see BasicVariableExhaustiveSearchStrategy
 */
public abstract sealed class ExhaustiveSearchStrategy<Solution_> implements ExhaustiveSearchPhaseLifecycleListener<Solution_>
        permits BasicVariableExhaustiveSearchStrategy {

    protected boolean assertWorkingSolutionScoreFromScratch = false;
    protected boolean assertExpectedWorkingSolutionScore = false;

    public ExhaustiveSearchStepScope<Solution_> prepareStep(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        var expandableNodeQueue = phaseScope.getExpandableNodeQueue();
        var stepScope = new ExhaustiveSearchStepScope<>(phaseScope);
        var node = expandableNodeQueue.last();
        expandableNodeQueue.remove(node);
        stepScope.setExpandingNode(node);
        return stepScope;
    }

    public abstract void solveStep(ExhaustiveSearchStepScope<Solution_> stepScope);

    public void enableAssertions(EnvironmentMode environmentMode) {
        assertWorkingSolutionScoreFromScratch = environmentMode.isFullyAsserted();
        assertExpectedWorkingSolutionScore = environmentMode.isIntrusivelyAsserted();
    }

}
