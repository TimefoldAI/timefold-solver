package ai.timefold.solver.core.impl.exhaustivesearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeSet;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.ExhaustiveSearchDecider;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchLayer;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.move.generic.CompositeMove;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.preview.api.move.Move;

/**
 * Default implementation of {@link ExhaustiveSearchPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class DefaultExhaustiveSearchPhase<Solution_> extends AbstractPhase<Solution_>
        implements ExhaustiveSearchPhase<Solution_> {

    protected final Comparator<ExhaustiveSearchNode> nodeComparator;
    protected final EntitySelector<Solution_> entitySelector;
    protected final ExhaustiveSearchDecider<Solution_> decider;

    protected final boolean assertWorkingSolutionScoreFromScratch;
    protected final boolean assertExpectedWorkingSolutionScore;

    private DefaultExhaustiveSearchPhase(Builder<Solution_> builder) {
        super(builder);
        nodeComparator = builder.nodeComparator;
        entitySelector = builder.entitySelector;
        decider = builder.decider;

        assertWorkingSolutionScoreFromScratch = builder.assertWorkingSolutionScoreFromScratch;
        assertExpectedWorkingSolutionScore = builder.assertExpectedWorkingSolutionScore;
    }

    @Override
    public String getPhaseTypeString() {
        return "Exhaustive Search";
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var expandableNodeQueue = new TreeSet<>(nodeComparator);
        var phaseScope = new ExhaustiveSearchPhaseScope<>(solverScope, phaseIndex);
        phaseScope.setExpandableNodeQueue(expandableNodeQueue);
        phaseStarted(phaseScope);

        while (!expandableNodeQueue.isEmpty() && !phaseTermination.isPhaseTerminated(phaseScope)) {
            var stepScope = new ExhaustiveSearchStepScope<>(phaseScope);
            var node = expandableNodeQueue.last();
            expandableNodeQueue.remove(node);
            stepScope.setExpandingNode(node);
            stepStarted(stepScope);
            restoreWorkingSolution(stepScope);
            decider.expandNode(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        phaseEnded(phaseScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        entitySelector.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        entitySelector.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
        fillLayerList(phaseScope);
        initStartNode(phaseScope);
    }

    private void fillLayerList(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        var stepScope = new ExhaustiveSearchStepScope<>(phaseScope);
        entitySelector.stepStarted(stepScope);
        var entitySize = entitySelector.getSize();
        if (entitySize > Integer.MAX_VALUE) {
            throw new IllegalStateException("The entitySelector (" + entitySelector
                    + ") has an entitySize (" + entitySize
                    + ") which is higher than Integer.MAX_VALUE.");
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

    public void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        // Skip entitySelector.stepStarted(stepScope)
        decider.stepStarted(stepScope);
    }

    protected <Score_ extends Score<Score_>> void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope) {
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
        var compositeMove = CompositeMove.buildMove(restoreMoveList);
        phaseScope.getScoreDirector().executeMove(compositeMove);
        var startingStepScore = stepScope.<Score_> getStartingStepScore();
        phaseScope.getSolutionDescriptor().setScore(phaseScope.getWorkingSolution(),
                (startingStepScore == null ? null : startingStepScore.raw()));
        if (assertWorkingSolutionScoreFromScratch) {
            // In BRUTE_FORCE the stepScore can be null because it was not calculated
            if (stepScope.getStartingStepScore() != null) {
                phaseScope.assertPredictedScoreFromScratch(stepScope.<Score_> getStartingStepScore(), restoreMoveList);
            }
        }
        if (assertExpectedWorkingSolutionScore) {
            // In BRUTE_FORCE the stepScore can be null because it was not calculated
            if (stepScope.getStartingStepScore() != null) {
                phaseScope.assertExpectedWorkingScore(stepScope.<Score_> getStartingStepScore(), restoreMoveList);
            }
        }
    }

    public void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        // Skip entitySelector.stepEnded(stepScope)
        decider.stepEnded(stepScope);
        if (logger.isDebugEnabled()) {
            var phaseScope = stepScope.getPhaseScope();
            logger.debug("{}    ES step ({}), time spent ({}), treeId ({}), {} best score ({}), selected move count ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getTreeId(),
                    (stepScope.getBestScoreImproved() ? "new" : "   "),
                    phaseScope.getBestScore().raw(),
                    stepScope.getSelectedMoveCount());
        }
    }

    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        entitySelector.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
        phaseScope.endingNow();
        logger.info("{}Exhaustive Search phase ({}) ended: time spent ({}), best score ({}),"
                + " move evaluation speed ({}/sec), step total ({}).",
                logIndentation,
                phaseIndex,
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore().raw(),
                phaseScope.getPhaseMoveEvaluationSpeed(),
                phaseScope.getNextStepIndex());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        entitySelector.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final Comparator<ExhaustiveSearchNode> nodeComparator;
        private final EntitySelector<Solution_> entitySelector;
        private final ExhaustiveSearchDecider<Solution_> decider;

        private boolean assertWorkingSolutionScoreFromScratch = false;
        private boolean assertExpectedWorkingSolutionScore = false;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                Comparator<ExhaustiveSearchNode> nodeComparator, EntitySelector<Solution_> entitySelector,
                ExhaustiveSearchDecider<Solution_> decider) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.nodeComparator = nodeComparator;
            this.entitySelector = entitySelector;
            this.decider = decider;
        }

        @Override
        public Builder<Solution_> enableAssertions(EnvironmentMode environmentMode) {
            super.enableAssertions(environmentMode);
            assertWorkingSolutionScoreFromScratch = environmentMode.isFullyAsserted();
            assertExpectedWorkingSolutionScore = environmentMode.isIntrusivelyAsserted();
            return this;
        }

        @Override
        public DefaultExhaustiveSearchPhase<Solution_> build() {
            return new DefaultExhaustiveSearchPhase<>(this);
        }
    }
}
