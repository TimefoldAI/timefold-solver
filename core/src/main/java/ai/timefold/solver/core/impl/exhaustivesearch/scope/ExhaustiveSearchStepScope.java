package ai.timefold.solver.core.impl.exhaustivesearch.scope;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class ExhaustiveSearchStepScope<Solution_> extends AbstractStepScope<Solution_> {

    private final ExhaustiveSearchPhaseScope<Solution_> phaseScope;

    private ExhaustiveSearchNode expandingNode;
    private Long selectedMoveCount = null;

    public ExhaustiveSearchStepScope(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        this(phaseScope, phaseScope.getNextStepIndex());
    }

    public ExhaustiveSearchStepScope(ExhaustiveSearchPhaseScope<Solution_> phaseScope, int stepIndex) {
        super(stepIndex);
        this.phaseScope = phaseScope;
    }

    @Override
    public ExhaustiveSearchPhaseScope<Solution_> getPhaseScope() {
        return phaseScope;
    }

    public ExhaustiveSearchNode getExpandingNode() {
        return expandingNode;
    }

    public void setExpandingNode(ExhaustiveSearchNode expandingNode) {
        this.expandingNode = expandingNode;
    }

    public Score getStartingStepScore() {
        return expandingNode.getScore();
    }

    public Long getSelectedMoveCount() {
        return selectedMoveCount;
    }

    public void setSelectedMoveCount(Long selectedMoveCount) {
        this.selectedMoveCount = selectedMoveCount;
    }

    // ************************************************************************
    // Calculated methods
    // ************************************************************************

    public int getDepth() {
        return expandingNode.getDepth();
    }

    public String getTreeId() {
        return expandingNode.getTreeId();
    }

}
