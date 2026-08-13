package ai.timefold.solver.core.preview.api.neighborhood.example;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class PickLastValueMoveProviderTest {

    @Test
    void buildMoveStream_customScoringRuleNotExpressibleAsAJoin() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);

        var solution = TestdataSolution.generateSolution(2, 2);
        var e1 = solution.getEntityList().get(0);
        var e2 = solution.getEntityList().get(1);
        var firstValue = solution.getValueList().get(0);
        var lastValue = solution.getValueList().get(1);

        var context = NeighborhoodTester.build(new PickLastValueMoveProvider(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(
                Moves.change(variableMetaModel, e1, lastValue),
                Moves.change(variableMetaModel, e2, lastValue));
        context.producesNoneOf(
                Moves.change(variableMetaModel, e1, firstValue),
                Moves.change(variableMetaModel, e2, firstValue));
    }

}
