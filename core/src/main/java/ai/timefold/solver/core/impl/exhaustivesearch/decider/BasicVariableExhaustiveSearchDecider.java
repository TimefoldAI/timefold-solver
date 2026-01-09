package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.preview.api.move.Move;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BasicVariableExhaustiveSearchDecider<Solution_> extends AbstractExhaustiveSearchDecider<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicVariableExhaustiveSearchDecider.class);

    private final ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder;

    public BasicVariableExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder,
            MoveRepository<Solution_> moveRepository, boolean scoreBounderEnabled, ScoreBounder<?> scoreBounder) {
        super(logIndentation, bestSolutionRecaller, termination, moveRepository, scoreBounderEnabled, scoreBounder);
        this.manualEntityMimicRecorder = manualEntityMimicRecorder;
    }

    @Override
    public void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope) {
        var expandingNode = stepScope.getExpandingNode();
        manualEntityMimicRecorder.setRecordedEntity(expandingNode.getEntity());

        var moveIndex = 0;
        var phaseScope = stepScope.getPhaseScope();
        var moveLayer = phaseScope.getLayerList().get(expandingNode.getDepth() + 1);
        for (var move : getMoveRepository()) {
            var moveNode = new ExhaustiveSearchNode(moveLayer, expandingNode);
            moveIndex++;
            moveNode.setMove(move);
            // Do not filter out pointless moves, because the original value of the entity(s) is irrelevant.
            // If the original value is null and the variable allows unassigned values,
            // the move to null must be done too.
            doMove(stepScope, moveNode);
            phaseScope.addMoveEvaluationCount(move, 1);
            // TODO in the lowest level (and only in that level) QuitEarly can be useful
            // No QuitEarly because lower layers might be promising
            phaseScope.getSolverScope().checkYielding();
            if (getTermination().isPhaseTerminated(stepScope.getPhaseScope())) {
                break;
            }
        }
        stepScope.setSelectedMoveCount((long) moveIndex);
    }

    @SuppressWarnings("unchecked")
    private <Score_ extends Score<Score_>> void doMove(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode) {
        var scoreDirector = stepScope.<Score_> getScoreDirector();
        Move<Solution_> move = moveNode.getMove();
        var undoMove = scoreDirector.getMoveDirector().executeTemporary(move,
                (score, undo) -> {
                    processMove(stepScope, moveNode, score);
                    return undo;
                });
        moveNode.setUndoMove(undoMove);
        var executionPoint = SolverLifecyclePoint.of(stepScope, moveNode.getTreeId());
        if (assertExpectedUndoMoveScore) {
            var startingStepScore = stepScope.<Score_> getStartingStepScore();
            // In BRUTE_FORCE a stepScore can be null because it was not calculated
            if (startingStepScore != null) {
                scoreDirector.assertExpectedUndoMoveScore(move, startingStepScore, executionPoint);
            }
        }
        var nodeScore = moveNode.getScore();
        LOGGER.trace("{}        Move treeId ({}), score ({}), expandable ({}), move ({}).",
                getLogIndentation(), executionPoint.treeId(), nodeScore == null ? "null" : nodeScore, moveNode.isExpandable(),
                moveNode.getMove());
    }

    private <Score_ extends Score<Score_>> void processMove(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode, InnerScore<Score_> score) {
        if (isScoreBounderEnabled()) {
            processMoveWithScoreBounder(stepScope, moveNode);
        } else {
            processMoveWithoutScoreBounder(stepScope, moveNode, score);
        }
    }

    private <Score_ extends Score<Score_>> void processMoveWithoutScoreBounder(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode, InnerScore<Score_> score) {
        var phaseScope = stepScope.getPhaseScope();
        if (moveNode.isLastLayer()) {
            moveNode.setScore(score);
            if (assertMoveScoreFromScratch) {
                phaseScope.assertWorkingScoreFromScratch(score, moveNode.getMove());
            }
            getBestSolutionRecaller().processWorkingSolutionDuringMove(score, stepScope);
        } else {
            phaseScope.addExpandableNode(moveNode);
        }
    }

    @SuppressWarnings("unchecked")
    private <Score_ extends Score<Score_>> void processMoveWithScoreBounder(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode) {
        var phaseScope = stepScope.getPhaseScope();
        var innerScore = phaseScope.<Score_> calculateScore();
        moveNode.setScore(innerScore);
        if (assertMoveScoreFromScratch) {
            phaseScope.assertWorkingScoreFromScratch(innerScore, moveNode.getMove());
        }
        if (moveNode.isLastLayer()) {
            // There is no point in bounding a fully initialized score
            phaseScope.registerPessimisticBound(innerScore);
            getBestSolutionRecaller().processWorkingSolutionDuringMove(innerScore, stepScope);
        } else {
            var scoreDirector = phaseScope.<Score_> getScoreDirector();
            var castScoreBounder = this.<Score_> getScoreBounder();
            var optimisticBound = castScoreBounder.calculateOptimisticBound(scoreDirector, innerScore);
            moveNode.setOptimisticBound(optimisticBound);
            var bestPessimisticBound = (InnerScore<Score_>) phaseScope.getBestPessimisticBound();
            if (optimisticBound.compareTo(bestPessimisticBound) > 0) {
                // It's still worth investigating this node further (no need to prune it)
                phaseScope.addExpandableNode(moveNode);
                var pessimisticBound = castScoreBounder.calculatePessimisticBound(scoreDirector, innerScore);
                phaseScope.registerPessimisticBound(pessimisticBound);
            }
        }
    }

}
