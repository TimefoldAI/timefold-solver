package ai.timefold.solver.core.impl.exhaustivesearch;

import java.util.ArrayList;
import java.util.Collections;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.AbstractExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

public final class BasicVariableExhaustiveSearchStrategy<Solution_> extends ExhaustiveSearchStrategy<Solution_> {

    private final EntitySelector<Solution_> entitySelector;
    private final AbstractExhaustiveSearchDecider<Solution_> decider;

    public BasicVariableExhaustiveSearchStrategy(EntitySelector<Solution_> entitySelector,
            AbstractExhaustiveSearchDecider<Solution_> decider) {
        this.entitySelector = entitySelector;
        this.decider = decider;
    }

    @Override
    public void solveStep(ExhaustiveSearchStepScope<Solution_> stepScope) {
        restoreWorkingSolution(stepScope);
        decider.expandNode(stepScope);
    }

    private void fillLayerList(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        var stepScope = new ExhaustiveSearchStepScope<>(phaseScope);
        entitySelector.stepStarted(stepScope);
        var entitySize = entitySelector.getSize();
        if (entitySize > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "The entitySelector (%s) has an entitySize (%d) which is higher than Integer.MAX_VALUE."
                            .formatted(entitySelector, entitySize));
        }
        var layerList = new ArrayList<ExhaustiveSearchLayer>((int) entitySize);
        var depth = 0;
        for (var entity : entitySelector) {
            var layer = new ExhaustiveSearchLayer(depth, entity);
            // Keep in sync with ExhaustiveSearchPhaseConfig.buildMoveSelectorConfig()
            // which includes all genuineVariableDescriptors
            var reinitializeVariableCount = entitySelector.getEntityDescriptor().countReinitializableVariables(entity);
            // Ignore entities with only initialized variables to avoid confusing bound decisions
            if (reinitializeVariableCount == 0) {
                continue;
            }
            depth++;
            layerList.add(layer);
        }
        var lastLayer = new ExhaustiveSearchLayer(depth, null);
        layerList.add(lastLayer);
        entitySelector.stepEnded(stepScope);
        phaseScope.setLayerList(layerList);
    }

    private <Score_ extends Score<Score_>> void initStartNode(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        var startLayer = phaseScope.getLayerList().get(0);
        var startNode = new ExhaustiveSearchNode(startLayer, null);

        if (decider.isScoreBounderEnabled()) {
            var scoreDirector = phaseScope.<Score_> getScoreDirector();
            var score = scoreDirector.calculateScore();
            startNode.setScore(score);
            var scoreBounder = decider.<Score_> getScoreBounder();
            phaseScope.setBestPessimisticBound(startLayer.isLastLayer() ? score
                    : scoreBounder.calculatePessimisticBound(scoreDirector, score));
            startNode.setOptimisticBound(startLayer.isLastLayer() ? score
                    : scoreBounder.calculateOptimisticBound(scoreDirector, score));
        }
        if (!startLayer.isLastLayer()) {
            phaseScope.addExpandableNode(startNode);
        }
        phaseScope.getLastCompletedStepScope().setExpandingNode(startNode);
    }

    <Score_ extends Score<Score_>> void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var oldNode = phaseScope.getLastCompletedStepScope().getExpandingNode();
        var newNode = stepScope.getExpandingNode();
        var oldMoveList = new ArrayList<Move<Solution_>>(oldNode.getDepth());
        var newMoveList = new ArrayList<Move<Solution_>>(newNode.getDepth());
        while (oldNode != newNode) {
            var oldDepth = oldNode.getDepth();
            var newDepth = newNode.getDepth();
            if (oldDepth < newDepth) {
                newMoveList.add(newNode.getMove());
                newNode = newNode.getParent();
            } else {
                oldMoveList.add(oldNode.getUndoMove());
                oldNode = oldNode.getParent();
            }
        }
        var restoreMoveList = new ArrayList<Move<Solution_>>(oldMoveList.size() + newMoveList.size());
        restoreMoveList.addAll(oldMoveList);
        Collections.reverse(newMoveList);
        restoreMoveList.addAll(newMoveList);
        if (restoreMoveList.isEmpty()) {
            // No moves to restore, so the working solution is already correct
            return;
        }
        var compositeMove = Moves.compose(restoreMoveList);
        phaseScope.getScoreDirector().executeMove(compositeMove);
        var startingStepScore = stepScope.<Score_> getStartingStepScore();
        phaseScope.getSolutionDescriptor().setScore(phaseScope.getWorkingSolution(),
                (startingStepScore == null ? null : startingStepScore.raw()));
        if (assertWorkingSolutionScoreFromScratch && stepScope.getStartingStepScore() != null) {
            // In BRUTE_FORCE the stepScore can be null because it was not calculated
            phaseScope.assertPredictedScoreFromScratch(stepScope.<Score_> getStartingStepScore(), restoreMoveList);
        }

        if (assertExpectedWorkingSolutionScore && stepScope.getStartingStepScore() != null) {
            // In BRUTE_FORCE the stepScore can be null because it was not calculated
            phaseScope.assertExpectedWorkingScore(stepScope.<Score_> getStartingStepScore(), restoreMoveList);
        }

    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        entitySelector.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        entitySelector.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

    @Override
    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        entitySelector.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
        fillLayerList(phaseScope);
        initStartNode(phaseScope);
    }

    @Override
    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        entitySelector.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
    }

    @Override
    public void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        // Skip entitySelector.stepStarted(stepScope)
        decider.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        // Skip entitySelector.stepEnded(stepScope)
        decider.stepEnded(stepScope);
    }
}
