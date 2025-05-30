package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.Iterator;
import java.util.Random;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.ChangeMove;

public class MixedCustomMoveIteratorFactory
        implements MoveIteratorFactory<TestdataMixedSolution, ChangeMove<TestdataMixedSolution>> {
    @Override
    public long getSize(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        return scoreDirector.getWorkingSolution().getEntityList().size();
    }

    @Override
    public Iterator<ChangeMove<TestdataMixedSolution>>
            createOriginalMoveIterator(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<ChangeMove<TestdataMixedSolution>>
            createRandomMoveIterator(ScoreDirector<TestdataMixedSolution> scoreDirector, Random workingRandom) {
        var solutionDescriptor = TestdataMixedSolution.buildSolutionDescriptor();
        var variableDescriptor =
                solutionDescriptor.findEntityDescriptor(TestdataMixedEntity.class).getGenuineVariableDescriptor("basicValue");
        var move1 = new ChangeMove<>(variableDescriptor, scoreDirector.getWorkingSolution().getEntityList().get(0),
                scoreDirector.getWorkingSolution().getOtherValueList().get(0));
        var move2 = new ChangeMove<>(variableDescriptor, scoreDirector.getWorkingSolution().getEntityList().get(0),
                scoreDirector.getWorkingSolution().getOtherValueList().get(1));
        return Stream.of(move1, move2).iterator();
    }
}
