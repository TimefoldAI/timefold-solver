package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

public final class BasicVariableExhaustiveSearchDecider<Solution_, Score_ extends Score<Score_>>
        extends AbstractExhaustiveSearchDecider<Solution_, Score_> {

    public BasicVariableExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, EntitySelector<Solution_> sourceEntitySelector,
            ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder, MoveRepository<Solution_> moveRepository,
            boolean scoreBounderEnabled, ScoreBounder<?> scoreBounder) {
        super(logIndentation, bestSolutionRecaller, termination, sourceEntitySelector, manualEntityMimicRecorder,
                moveRepository, scoreBounderEnabled, scoreBounder);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope) {
        var expandingNode = stepScope.getExpandingNode();
        manualEntityMimicRecorder.setRecordedEntity(expandingNode.getEntity());
        var moveIndex = new MutableInt(0);
        if (expandingNode.getLayer().isLastLayer()) {
            // Mixed models will always return false when checking if the solution is complete
            // because the list variable will be resolved later.
            // Therefore, we need to ensure that processing the move occurs in the last layer
            // when there are no more possible moves available
            moveIndex.increment();
            doMove(stepScope, expandingNode, true, true);
            stepScope.getPhaseScope().addMoveEvaluationCount(expandingNode.getMove(), 1);
        } else {
            var moveLayer = stepScope.getPhaseScope().getLayerList().get(expandingNode.getDepth() + 1);
            expandNode(stepScope, expandingNode, moveLayer, moveIndex);
            stepScope.setSelectedMoveCount(moveIndex.longValue());
        }
    }

    @Override
    public boolean isSolutionComplete(ExhaustiveSearchNode<Solution_> expandingNode) {
        return expandingNode.getLayer().isLastLayer();
    }

    @Override
    public boolean isEntityReinitializable(Object entity) {
        // Keep in sync with ExhaustiveSearchPhaseConfig.buildMoveSelectorConfig()
        // which includes all genuineVariableDescriptors
        var reinitializeVariableCount = sourceEntitySelector.getEntityDescriptor().countReinitializableVariables(entity);
        // Ignore entities with only initialized variables to avoid confusing bound decisions
        return reinitializeVariableCount > 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope,
            boolean assertWorkingSolutionScoreFromScratch, boolean assertExpectedWorkingSolutionScore) {
        var phaseScope = stepScope.getPhaseScope();
        var oldNode = phaseScope.getLastCompletedStepScope().getExpandingNode();
        var newNode = stepScope.getExpandingNode();
        var oldMoveArray = new Move[oldNode.getDepth()];
        var newMoveArray = new Move[newNode.getDepth()];
        var oldMoveCount = 0;
        var newMoveCount = 0;
        while (oldNode != newNode) {
            var oldDepth = oldNode.getDepth();
            var newDepth = newNode.getDepth();
            if (oldDepth < newDepth) {
                newMoveArray[newMoveArray.length - newMoveCount++ - 1] = newNode.getMove(); // Build this in reverse.
                newNode = newNode.getParent();
            } else {
                oldMoveArray[oldMoveCount++] = oldNode.getUndoMove();
                oldNode = oldNode.getParent();
            }
        }
        var totalCount = newMoveCount + oldMoveCount;
        if (totalCount == 0) {
            // No moves to restore, so the working solution is already correct.
            return;
        }
        // Build a composite move of both arrays.
        var moves = Arrays.copyOf(oldMoveArray, totalCount);
        System.arraycopy(newMoveArray, newMoveArray.length - newMoveCount, moves, oldMoveCount, newMoveCount);
        var restoreMoveList = Arrays.<Move<Solution_>> asList(moves);
        var compositeMove = Moves.compose(restoreMoveList);
        // Execute the move.
        phaseScope.getScoreDirector().executeMove(compositeMove);
        var startingStepScore = stepScope.<Score_> getStartingStepScore();
        phaseScope.getSolutionDescriptor().setScore(phaseScope.getWorkingSolution(),
                (startingStepScore == null ? null : startingStepScore.raw()));

        // In BRUTE_FORCE the stepScore can be null because it was not calculated
        if (assertWorkingSolutionScoreFromScratch && stepScope.getStartingStepScore() != null) {
            phaseScope.assertPredictedScoreFromScratch(startingStepScore, restoreMoveList);
        }

        // In BRUTE_FORCE the stepScore can be null because it was not calculated
        if (assertExpectedWorkingSolutionScore && stepScope.getStartingStepScore() != null) {
            phaseScope.assertExpectedWorkingScore(startingStepScore, restoreMoveList);
        }
    }

}
