package ai.timefold.solver.core.testdomain.mixed.singleentity;

import java.util.Iterator;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.move.factory.MoveIteratorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;

public class MixedCustomMoveIteratorFactory
        implements MoveIteratorFactory<TestdataMixedSolution, SelectorBasedChangeMove<TestdataMixedSolution>> {
    @Override
    public long getSize(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        return scoreDirector.getWorkingSolution().getEntityList().size();
    }

    @Override
    public Iterator<SelectorBasedChangeMove<TestdataMixedSolution>>
            createOriginalMoveIterator(ScoreDirector<TestdataMixedSolution> scoreDirector) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<SelectorBasedChangeMove<TestdataMixedSolution>>
            createRandomMoveIterator(ScoreDirector<TestdataMixedSolution> scoreDirector, RandomGenerator workingRandom) {
        var solutionDescriptor = TestdataMixedSolution.buildSolutionDescriptor();
        var variableDescriptor =
                solutionDescriptor.findEntityDescriptor(TestdataMixedEntity.class).getGenuineVariableDescriptor("basicValue");
        var move1 =
                new SelectorBasedChangeMove<>(variableDescriptor, scoreDirector.getWorkingSolution().getEntityList().getFirst(),
                        scoreDirector.getWorkingSolution().getOtherValueList().getFirst());
        var move2 =
                new SelectorBasedChangeMove<>(variableDescriptor, scoreDirector.getWorkingSolution().getEntityList().getFirst(),
                        scoreDirector.getWorkingSolution().getOtherValueList().get(1));
        return Stream.of(move1, move2).iterator();
    }
}
