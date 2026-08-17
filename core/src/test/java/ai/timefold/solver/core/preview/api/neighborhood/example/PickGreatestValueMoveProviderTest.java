package ai.timefold.solver.core.preview.api.neighborhood.example;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class PickGreatestValueMoveProviderTest {

    @Test
    void buildMoveStream_customScoringRuleNotExpressibleAsAJoin() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var solution = TestdataSolution.generateSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var smallestValue = solution.getValueList().get(0);
        var greatestValue = solution.getValueList().get(1);

        var context = NeighborhoodTester.build(new PickGreatestValueMoveProvider(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, e1, greatestValue),
                Moves.change(variableMetaModel, e2, greatestValue));
        context.producesNoneOf(
                Moves.change(variableMetaModel, e1, smallestValue),
                Moves.change(variableMetaModel, e2, smallestValue));
    }

}
