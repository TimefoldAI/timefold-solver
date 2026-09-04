package ai.timefold.solver.core.preview.api.neighborhood.example;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SwapValuesInCodeRangeMoveProviderTest {

    @Test
    void producesASwapWithinTheOnlyRange() {
        var solutionMetaModel = TestdataSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataEntity.class)
                .<TestdataValue> basicVariable();

        // 3 values, 2 entities: e0 = v0, e1 = v1;
        // v2 is never assigned to any entity.
        var solution = TestdataSolution.generateSolution(3, 2);
        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);
        var v0 = solution.getValueList().get(0);
        var v1 = solution.getValueList().get(1);
        var v2 = solution.getValueList().get(2);

        var context = NeighborhoodTester.build(new SwapValuesInCodeRangeMoveProvider(variableMetaModel),
                solutionMetaModel).using(solution);

        // Only one range exists (e0..e1) and only e0 holds v0, so of the (v0, v2) pair,
        // only e0 -> v2 fires:
        // v2 is unassigned, so its own direction (v2 -> v0) contributes nothing,
        // and compose() of a single move returns that move directly rather than wrapping it in a CompositeMove.
        context.producesAllOf(Moves.change(variableMetaModel, e0, v2));

        // No-op changes are structurally impossible: a value pair is always two distinct values,
        // so an entity is never reassigned to the value it already holds.
        context.producesNoneOf(
                Moves.change(variableMetaModel, e0, v0),
                Moves.change(variableMetaModel, e1, v1));
    }

}
