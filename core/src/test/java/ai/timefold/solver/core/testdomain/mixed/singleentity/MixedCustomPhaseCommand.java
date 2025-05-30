package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.function.BooleanSupplier;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.phase.PhaseCommand;

public class MixedCustomPhaseCommand implements PhaseCommand<TestdataMixedSolution> {

    @Override
    public void changeWorkingSolution(ScoreDirector<TestdataMixedSolution> scoreDirector, BooleanSupplier isPhaseTerminated) {
        var moveIteratorFactory = new MixedCustomMoveIteratorFactory();
        var moveIterator = moveIteratorFactory.createRandomMoveIterator(scoreDirector, null);
        var move = moveIterator.next();
        move.doMoveOnly(scoreDirector);
        scoreDirector.triggerVariableListeners();
    }
}
