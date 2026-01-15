package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import java.util.ArrayList;
import java.util.Collections;

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

public final class BasicExhaustiveSearchDecider<Solution_, Score_ extends Score<Score_>>
        extends AbstractExhaustiveSearchDecider<Solution_, Score_> {

    public BasicExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
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
        // Expand only the current selected node for basic variables
        var moveLayer = stepScope.getPhaseScope().getLayerList().get(expandingNode.getDepth() + 1);
        expandNode(stepScope, expandingNode, moveLayer, moveIndex);
        stepScope.setSelectedMoveCount(moveIndex.longValue());
    }

    @Override
    public boolean isSolutionComplete(ExhaustiveSearchNode expandingNode) {
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

    @Override
    public void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope,
            boolean assertWorkingSolutionScoreFromScratch, boolean assertExpectedWorkingSolutionScore) {
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
