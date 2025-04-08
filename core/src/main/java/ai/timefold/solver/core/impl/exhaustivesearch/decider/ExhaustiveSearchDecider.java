package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.event.ExhaustiveSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.move.MoveRepository;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExhaustiveSearchDecider<Solution_> implements ExhaustiveSearchPhaseLifecycleListener<Solution_> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExhaustiveSearchDecider.class);

    private final String logIndentation;
    private final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    private final PhaseTermination<Solution_> termination;
    private final ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder;
    private final MoveRepository<Solution_> moveRepository;
    private final boolean scoreBounderEnabled;
    private final ScoreBounder<?> scoreBounder;

    private boolean assertMoveScoreFromScratch = false;
    private boolean assertExpectedUndoMoveScore = false;

    public ExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder,
            MoveRepository<Solution_> moveRepository, boolean scoreBounderEnabled, ScoreBounder<?> scoreBounder) {
        this.logIndentation = logIndentation;
        this.bestSolutionRecaller = bestSolutionRecaller;
        this.termination = termination;
        this.manualEntityMimicRecorder = manualEntityMimicRecorder;
        this.moveRepository = moveRepository;
        this.scoreBounderEnabled = scoreBounderEnabled;
        this.scoreBounder = scoreBounder;
    }

    public MoveRepository<Solution_> getMoveRepository() {
        return moveRepository;
    }

    public boolean isScoreBounderEnabled() {
        return scoreBounderEnabled;
    }

    @SuppressWarnings("unchecked")
    public <Score_ extends Score<Score_>> ScoreBounder<Score_> getScoreBounder() {
        return (ScoreBounder<Score_>) scoreBounder;
    }

    public void setAssertMoveScoreFromScratch(boolean assertMoveScoreFromScratch) {
        this.assertMoveScoreFromScratch = assertMoveScoreFromScratch;
    }

    public void setAssertExpectedUndoMoveScore(boolean assertExpectedUndoMoveScore) {
        this.assertExpectedUndoMoveScore = assertExpectedUndoMoveScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveRepository.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseStarted(phaseScope);
    }

    @Override
    public void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        moveRepository.stepStarted(stepScope);
    }

    @Override
    public void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        moveRepository.stepEnded(stepScope);
    }

    @Override
    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseEnded(phaseScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        moveRepository.solvingEnded(solverScope);
    }

    public void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope) {
        var expandingNode = stepScope.getExpandingNode();
        manualEntityMimicRecorder.setRecordedEntity(expandingNode.getEntity());

        var moveIndex = 0;
        var phaseScope = stepScope.getPhaseScope();
        var moveLayer = phaseScope.getLayerList().get(expandingNode.getDepth() + 1);
        for (var move : moveRepository) {
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
            if (termination.isPhaseTerminated(stepScope.getPhaseScope())) {
                break;
            }
        }
        stepScope.setSelectedMoveCount((long) moveIndex);
    }

    @SuppressWarnings("unchecked")
    private <Score_ extends Score<Score_>> void doMove(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode) {
        var scoreDirector = stepScope.<Score_> getScoreDirector();
        var move = moveNode.getMove();
        var undoMove = scoreDirector.getMoveDirector().executeTemporary(move,
                (score, undo) -> {
                    processMove(stepScope, moveNode);
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
                logIndentation, executionPoint.treeId(), nodeScore == null ? "null" : nodeScore, moveNode.isExpandable(),
                moveNode.getMove());
    }

    @SuppressWarnings("unchecked")
    private <Score_ extends Score<Score_>> void processMove(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode) {
        var phaseScope = stepScope.getPhaseScope();
        var lastLayer = moveNode.isLastLayer();
        if (!scoreBounderEnabled) {
            if (lastLayer) {
                var score = phaseScope.<Score_> calculateScore();
                moveNode.setScore(score);
                if (assertMoveScoreFromScratch) {
                    phaseScope.assertWorkingScoreFromScratch(score, moveNode.getMove());
                }
                bestSolutionRecaller.processWorkingSolutionDuringMove(score, stepScope);
            } else {
                phaseScope.addExpandableNode(moveNode);
            }
        } else {
            var innerScore = phaseScope.<Score_> calculateScore();
            moveNode.setScore(innerScore);
            if (assertMoveScoreFromScratch) {
                phaseScope.assertWorkingScoreFromScratch(innerScore, moveNode.getMove());
            }
            if (lastLayer) {
                // There is no point in bounding a fully initialized score
                phaseScope.registerPessimisticBound(innerScore);
                bestSolutionRecaller.processWorkingSolutionDuringMove(innerScore, stepScope);
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

}
