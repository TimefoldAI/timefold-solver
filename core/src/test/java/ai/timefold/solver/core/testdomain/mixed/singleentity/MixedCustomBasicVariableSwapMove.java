package ai.timefold.solver.core.testdomain.mixed.singleentity;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.move.AbstractMove;

public class MixedCustomBasicVariableSwapMove extends AbstractMove<TestdataMixedSolution> {

    private final TestdataMixedEntity left;
    private final TestdataMixedEntity right;

    public MixedCustomBasicVariableSwapMove(TestdataMixedEntity left, TestdataMixedEntity right) {
        this.left = left;
        this.right = right;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        scoreDirector.beforeVariableChanged(left, "basicValue");
        scoreDirector.beforeVariableChanged(right, "basicValue");
        var oldValue = left.getBasicValue();
        left.setBasicValue(right.getBasicValue());
        right.setBasicValue(oldValue);
        scoreDirector.afterVariableChanged(left, "basicValue");
        scoreDirector.afterVariableChanged(right, "basicValue");
    }

    @Override
    public boolean isMoveDoable(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        return left != right;
    }
}
