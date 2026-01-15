package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import java.util.ArrayList;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.event.ExhaustiveSearchPhaseLifecycleListener;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.preview.api.move.Move;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract sealed class AbstractExhaustiveSearchDecider<Solution_, Score_ extends Score<Score_>>
        implements ExhaustiveSearchPhaseLifecycleListener<Solution_>
        permits ListVariableExhaustiveSearchDecider, BasicExhaustiveSearchDecider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExhaustiveSearchDecider.class);

    private final String logIndentation;
    private final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    private final PhaseTermination<Solution_> termination;
    protected final EntitySelector<Solution_> sourceEntitySelector;
    protected final ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder;
    private final MoveRepository<Solution_> moveRepository;
    protected final boolean scoreBounderEnabled;
    private final ScoreBounder<?> scoreBounder;

    private boolean assertMoveScoreFromScratch = false;
    private boolean assertExpectedUndoMoveScore = false;

    AbstractExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, EntitySelector<Solution_> sourceEntitySelector,
            ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder, MoveRepository<Solution_> moveRepository,
            boolean scoreBounderEnabled, ScoreBounder<?> scoreBounder) {
        this.logIndentation = logIndentation;
        this.bestSolutionRecaller = bestSolutionRecaller;
        this.termination = termination;
        this.sourceEntitySelector = sourceEntitySelector;
        this.manualEntityMimicRecorder = manualEntityMimicRecorder;
        this.moveRepository = moveRepository;
        this.scoreBounderEnabled = scoreBounderEnabled;
        this.scoreBounder = scoreBounder;
    }

    @SuppressWarnings("unchecked")
    public ScoreBounder<Score_> getScoreBounder() {
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

    public abstract void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope);

    public abstract boolean isSolutionComplete(ExhaustiveSearchNode expandingNode);

    public abstract void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope,
            boolean assertWorkingSolutionScoreFromScratch, boolean assertExpectedWorkingSolutionScore);

    public abstract boolean isEntityReinitializable(Object entity);

    protected void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope, ExhaustiveSearchNode expandingNode,
            ExhaustiveSearchLayer moveLayer, MutableInt moveIndex) {
        var phaseScope = stepScope.getPhaseScope();
        for (var move : moveRepository) {
            var moveNode = new ExhaustiveSearchNode(moveLayer, expandingNode);
            moveIndex.increment();
            moveNode.setMove(move);
            // Do not filter out pointless moves, because the original value of the entity(s) is irrelevant.
            // If the original value is null and the variable allows unassigned values,
            // the move to null must be done too.
            doMove(stepScope, moveNode, isSolutionComplete(moveNode), false);
            phaseScope.addMoveEvaluationCount(move, 1);
            // TODO in the lowest level (and only in that level) QuitEarly can be useful
            // No QuitEarly because lower layers might be promising
            phaseScope.getSolverScope().checkYielding();
            if (termination.isPhaseTerminated(stepScope.getPhaseScope())) {
                break;
            }
        }
    }

    protected void doMove(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode, boolean isSolutionComplete, boolean skipMoveExecution) {
        var scoreDirector = stepScope.<Score_> getScoreDirector();
        Move<Solution_> move = moveNode.getMove();
        if (!skipMoveExecution) {
            var undoMove = scoreDirector.getMoveDirector().executeTemporary(move,
                    (score, undo) -> {
                        processMove(stepScope, moveNode, isSolutionComplete, score);
                        return undo;
                    });
            moveNode.setUndoMove(undoMove);
        }
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

    private void processMove(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode, boolean isSolutionComplete, InnerScore<Score_> score) {
        if (!scoreBounderEnabled) {
            processMoverWithoutBounder(stepScope, moveNode, isSolutionComplete, score);
        } else {
            processMoverWithBounder(stepScope, moveNode, isSolutionComplete);
        }
    }

    private void processMoverWithoutBounder(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode, boolean isSolutionComplete, InnerScore<Score_> score) {
        var phaseScope = stepScope.getPhaseScope();
        if (isSolutionComplete) {
            moveNode.setScore(score);
            if (assertMoveScoreFromScratch) {
                phaseScope.assertWorkingScoreFromScratch(score, moveNode.getMove());
            }
            bestSolutionRecaller.processWorkingSolutionDuringMove(score, stepScope);
        } else {
            phaseScope.addExpandableNode(moveNode);
        }
    }

    private void processMoverWithBounder(ExhaustiveSearchStepScope<Solution_> stepScope,
            ExhaustiveSearchNode moveNode, boolean isSolutionComplete) {
        var phaseScope = stepScope.getPhaseScope();
        var innerScore = phaseScope.<Score_> calculateScore();
        moveNode.setScore(innerScore);
        if (assertMoveScoreFromScratch) {
            phaseScope.assertWorkingScoreFromScratch(innerScore, moveNode.getMove());
        }
        if (isSolutionComplete) {
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

    private void fillLayerList(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        var stepScope = new ExhaustiveSearchStepScope<>(phaseScope);
        sourceEntitySelector.stepStarted(stepScope);
        var entitySize = sourceEntitySelector.getSize();
        if (entitySize > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "The entitySelector (%s) has an entitySize (%d) which is higher than Integer.MAX_VALUE."
                            .formatted(sourceEntitySelector, entitySize));
        }
        var layerList = new ArrayList<ExhaustiveSearchLayer>((int) entitySize);
        var depth = 0;
        for (var entity : sourceEntitySelector) {
            var layer = new ExhaustiveSearchLayer(depth, entity);
            if (!isEntityReinitializable(entity)) {
                continue;
            }
            depth++;
            layerList.add(layer);
        }
        var lastLayer = new ExhaustiveSearchLayer(depth, null);
        layerList.add(lastLayer);
        sourceEntitySelector.stepEnded(stepScope);
        phaseScope.setLayerList(layerList);
    }

    protected void initStartNode(ExhaustiveSearchPhaseScope<Solution_> phaseScope,
            ExhaustiveSearchLayer layer) {
        var startLayer = layer == null ? phaseScope.getLayerList().get(0) : layer;
        var startNode = new ExhaustiveSearchNode(startLayer, null);

        if (scoreBounderEnabled) {
            var scoreDirector = phaseScope.<Score_> getScoreDirector();
            var score = scoreDirector.calculateScore();
            startNode.setScore(score);
            ScoreBounder<Score_> bounder = getScoreBounder();
            phaseScope.setBestPessimisticBound(startLayer.isLastLayer() ? score
                    : bounder.calculatePessimisticBound(scoreDirector, score));
            startNode.setOptimisticBound(startLayer.isLastLayer() ? score
                    : bounder.calculateOptimisticBound(scoreDirector, score));
        }
        if (!startLayer.isLastLayer()) {
            phaseScope.addExpandableNode(startNode);
        }
        phaseScope.getLastCompletedStepScope().setExpandingNode(startNode);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveRepository.solvingStarted(solverScope);
    }

    @Override
    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseStarted(phaseScope);
        fillLayerList(phaseScope);
        initStartNode(phaseScope, null);
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

}
