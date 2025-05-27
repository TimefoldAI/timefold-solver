package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.Iterator;
import java.util.Random;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;

public class MixedCustomBasicVariableFactory
        implements MoveIteratorFactory<TestdataMixedSolution, MixedCustomBasicVariableSwapMove> {
    @Override
    public long getSize(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        return scoreDirector.getWorkingSolution().getEntityList().size();
    }

    @Override
    public Iterator<MixedCustomBasicVariableSwapMove>
            createOriginalMoveIterator(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<MixedCustomBasicVariableSwapMove>
            createRandomMoveIterator(ScoreDirector<TestdataMixedSolution> scoreDirector, Random workingRandom) {
        var move1 = new MixedCustomBasicVariableSwapMove(scoreDirector.getWorkingSolution().getEntityList().get(0),
                scoreDirector.getWorkingSolution().getEntityList().get(1));
        var move2 = new MixedCustomBasicVariableSwapMove(scoreDirector.getWorkingSolution().getEntityList().get(1),
                scoreDirector.getWorkingSolution().getEntityList().get(0));
        return Stream.of(move1, move2).iterator();
    }
}
