package ai.timefold.solver.core.impl.neighborhood;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import ai.timefold.solver.core.impl.move.DefaultMoveRunner;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListEntity;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListSolution;
import ai.timefold.solver.core.testdomain.shadow.simple_list.TestdataDeclarativeSimpleListValue;

import org.junit.jupiter.api.Test;

class NeighborhoodsNotificationTest {

    @Test
    void notifyWhenValueIndexChangesOnSimpleModel() {
        var neighborhoodMoveRepository = mock(NeighborhoodsBasedMoveRepository.class);
        var solutionMetamodel = TestdataDeclarativeSimpleListSolution.buildSolutionMetaModel();
        var variableMetamodel = solutionMetamodel.entity(TestdataDeclarativeSimpleListEntity.class)
                .listVariable("values", TestdataDeclarativeSimpleListValue.class);
        try (var moveRunner =
                new DefaultMoveRunner<TestdataDeclarativeSimpleListSolution>(solutionMetamodel, neighborhoodMoveRepository)) {
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

            moveRunner.using(solution).execute(Moves.change(variableMetamodel,
                    ElementPosition.of(entity1, 0),
                    ElementPosition.of(entity2, 0)));

            verify(neighborhoodMoveRepository, atLeastOnce()).update(value1);
            verify(neighborhoodMoveRepository, atLeastOnce()).update(value2);
            verify(neighborhoodMoveRepository, atLeastOnce()).update(value3);
        }
    }
}
