package ai.timefold.solver.core.preview.api.neighborhood.example;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SwapEntityTailsMoveProviderTest {

    @Test
    void producesExactSwapAtAKnownCutAndNoneForACutPastTheSharedLength() {
        var solutionMetaModel = TestdataListSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntity.class)
                .<TestdataListValue> listVariable();

        // 6 values round-robin over 2 entities: e0=[v0,v2,v4], e1=[v1,v3,v5].
        // Both lists have 3 elements.
        var solution = TestdataListSolution.generateInitializedSolution(6, 2);
        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);

        var context = NeighborhoodTester.build(new SwapEntityTailsMoveProvider(variableMetaModel), solutionMetaModel)
                .using(solution);
        context.producesAllOf(new SwapEntityTailsMove(variableMetaModel, e0, e1, 1));

        // A cut at the shared length (3) leaves nothing to swap;
        // it is never produced.
        context.producesNoneOf(new SwapEntityTailsMove(variableMetaModel, e0, e1, 3));
    }

}
