package ai.timefold.solver.core.impl.neighborhood;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListEntity;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListValue;

import org.junit.jupiter.api.Test;

class NeighborhoodsNotificationTest {

    @Test
    void notifyWhenValueIndexChangesOnSimpleModel() {
        var solution = new TestdataDeclarativeSimpleListSolution();
        var entity1 = new TestdataDeclarativeSimpleListEntity("e1", 0, 0);
        var entity2 = new TestdataDeclarativeSimpleListEntity("e2", 0, 0);

        var value1 = new TestdataDeclarativeSimpleListValue("v1", 0, 0);
        var value2 = new TestdataDeclarativeSimpleListValue("v2", 0, 0);
        var value3 = new TestdataDeclarativeSimpleListValue("v3", 0, 0);

        entity1.getValues().add(value1);
        entity2.getValues().add(value2);
        entity2.getValues().add(value3);

        solution.setEntityList(List.of(entity1, entity2));
        solution.setValueList(List.of(value1, value2, value3));

        var neighborhoodMoveRepository = mock(NeighborhoodsBasedMoveRepository.class);
        var solutionDescriptor = TestdataDeclarativeSimpleListSolution.buildSolutionDescriptor();
        var solutionMetamodel = solutionDescriptor.getMetaModel();
        var variableMetamodel = solutionMetamodel.genuineEntity(TestdataDeclarativeSimpleListEntity.class)
                .listVariable("values", TestdataDeclarativeSimpleListValue.class);
        try (var scoreDirector = new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor,
                constraintFactory -> new Constraint[] {
                        constraintFactory.forEach(Object.class)
                                .penalize(SimpleScore.ONE)
                                .asConstraint("dummy constraint")
                }, EnvironmentMode.FULL_ASSERT)
                .buildScoreDirector()) {
            scoreDirector.setMoveRepository(neighborhoodMoveRepository);
            scoreDirector.setWorkingSolution(solution);
            scoreDirector.calculateScore();

            var move = Moves.change(variableMetamodel, ElementPosition.of(entity1, 0), ElementPosition.of(entity2, 0));
            scoreDirector.executeMove(move);
            scoreDirector.calculateScore();
        }

        verify(neighborhoodMoveRepository, atLeastOnce()).update(value1);
        verify(neighborhoodMoveRepository, atLeastOnce()).update(value2);
        verify(neighborhoodMoveRepository, atLeastOnce()).update(value3);
    }

}
