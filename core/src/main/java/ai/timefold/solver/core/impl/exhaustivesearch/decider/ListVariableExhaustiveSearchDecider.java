package ai.timefold.solver.core.impl.exhaustivesearch.decider;

import java.util.Arrays;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.bounder.ScoreBounder;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.ManualEntityMimicRecorder;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.util.MutableInt;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;

public final class ListVariableExhaustiveSearchDecider<Solution_, Score_ extends Score<Score_>>
        extends AbstractExhaustiveSearchDecider<Solution_, Score_> {

    private ListVariableStateSupply<Solution_, ?, ?> listVariableState;

    public ListVariableExhaustiveSearchDecider(String logIndentation, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            PhaseTermination<Solution_> termination, EntitySelector<Solution_> sourceEntitySelector,
            ManualEntityMimicRecorder<Solution_> manualEntityMimicRecorder, MoveRepository<Solution_> moveRepository,
            boolean scoreBounderEnabled, ScoreBounder<?> scoreBounder) {
        super(logIndentation, bestSolutionRecaller, termination, sourceEntitySelector, manualEntityMimicRecorder,
                moveRepository, scoreBounderEnabled, scoreBounder);
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    /**
     * The method updates the exploration of the solution's space when using a list variable.
     * The logic will start adding all possible search nodes that belong to all available layers.
     * For example, considering two entities {@code [e0, e1]} and the values {@code [v0, v1]},
     * the result is: {@code (layer 0, e0, [v0]), (layer 0, e0, [v1]), (layer 1, e1, [v0]), (layer 1, e1, [v1])}.
     * From these initial nodes, the method will be able to explore all possible solutions.
     * <p>
     * When a solver adds another value to a list, the layer remains unchanged, ensuring that the depth remains equal.
     * This allows the solver to correctly sort the nodes using either breadth-first or depth-first approaches.
     * For example, the following search node has three values in the list {@code e0[v0, v1, v2]},
     * and its node tree is given as follows:
     * <p>
     *
     * <pre>{@code
     *  (layer 0, e0, [v0])
     *              |
     *              |
     *      (layer 0, e0, [v1])
     *                  |
     *                  |
     *          (layer 0, e0, [v2])
     *  }</pre>
     * <p>
     * After selecting a search node, all potential moves for that node's layer and the subsequent layers are generated.
     * This step is essential to prevent the reevaluation of already visited solutions.
     * It's important to note that the previous layers have already evaluated all possible permutations.
     */
    @Override
    public void expandNode(ExhaustiveSearchStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var expandingNode = stepScope.getExpandingNode();
        // We need to make sure that all layers following the current one are evaluated
        var moveIndex = new MutableInt(0);
        // There are no more values available
        if (listVariableState.getUnassignedCount() == 0) {
            moveIndex.increment();
            doMove(stepScope, expandingNode, true, true);
            phaseScope.addMoveEvaluationCount(expandingNode.getMove(), 1);
        } else {
            for (var i = expandingNode.getLayer().getDepth(); i < phaseScope.getLayerList().size(); i++) {
                var layer = phaseScope.getLayerList().get(i);
                if (layer.isLastLayer()) {
                    break;
                }
                manualEntityMimicRecorder.setRecordedEntity(layer.getEntity());
                expandNode(stepScope, expandingNode, layer, moveIndex);
                stepScope.setSelectedMoveCount(moveIndex.longValue());
            }
        }
    }

    @Override
    public boolean isSolutionComplete(ExhaustiveSearchNode<Solution_> expandingNode) {
        // One value to be assigned and one move to be done
        return listVariableState.getUnassignedCount() <= 1;
    }

    @Override
    public boolean isEntityReinitializable(Object entity) {
        // List variables are always initializable
        return true;
    }

    @Override
    public void restoreWorkingSolution(ExhaustiveSearchStepScope<Solution_> stepScope,
            boolean assertWorkingSolutionScoreFromScratch, boolean assertExpectedWorkingSolutionScore) {
        var phaseScope = stepScope.getPhaseScope();
        //First, undo all previous changes.
        var unassignMoves = listAllUndoMoves(phaseScope.getLastCompletedStepScope().getExpandingNode());
        // Next, rebuild the solution starting from the current search element.
        var assignMoves = listAllMovesInReverseOrder(stepScope.getExpandingNode());
        var totalLength = unassignMoves.length + assignMoves.length;
        if (totalLength == 0) {
            // No moves to restore, so the working solution is already correct.
            return;
        }
        // Build a composite move of both arrays.
        var moves = Arrays.copyOf(unassignMoves, unassignMoves.length + assignMoves.length);
        System.arraycopy(assignMoves, 0, moves, unassignMoves.length, assignMoves.length);
        var compositeMove = Moves.compose(moves);
        // Execute the move.
        phaseScope.getScoreDirector().executeMove(compositeMove);
        var score = phaseScope.<Score_> calculateScore();
        stepScope.getExpandingNode().setScore(score);
        phaseScope.getSolutionDescriptor().setScore(phaseScope.getWorkingSolution(), score.raw());
    }

    private static <Solution_> Move<Solution_>[] listAllUndoMoves(ExhaustiveSearchNode<Solution_> node) {
        return listAllUndoMoves(node, 0);
    }

    private static <Solution_> Move<Solution_>[] listAllUndoMoves(ExhaustiveSearchNode<Solution_> node, int depth) {
        var undoMove = node.getUndoMove();
        if (undoMove != null) {
            var array = listAllUndoMoves(node.getParent(), depth + 1);
            array[depth] = undoMove;
            return array;
        } else {
            return new Move[depth];
        }
    }

    private static <Solution_> Move<Solution_>[] listAllMovesInReverseOrder(ExhaustiveSearchNode<Solution_> node) {
        return listAllMovesInReverseOrder(node, 0);
    }

    private static <Solution_> Move<Solution_>[] listAllMovesInReverseOrder(ExhaustiveSearchNode<Solution_> node, int depth) {
        var move = node.getMove();
        if (move != null) {
            var array = listAllMovesInReverseOrder(node.getParent(), depth + 1);
            array[array.length - depth - 1] = move;
            return array;
        } else {
            return new Move[depth];
        }
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        var listVariableDescriptor = phaseScope.getSolutionDescriptor().getListVariableDescriptor();
        this.listVariableState =
                phaseScope.getSolverScope().getScoreDirector().getListVariableStateSupply(listVariableDescriptor);
    }

    @Override
    public void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        this.listVariableState = null;
    }
}
