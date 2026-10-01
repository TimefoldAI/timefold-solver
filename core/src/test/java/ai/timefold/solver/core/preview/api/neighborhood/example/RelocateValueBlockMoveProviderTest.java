package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class RelocateValueBlockMoveProviderTest {

    @Test
    void producesExactRelocationInRangeAndNoneOutOfRange() {
        var solutionMetaModel = TestdataListEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntityProvidingEntity.class)
                .<TestdataListEntityProvidingValue> listVariable();

        var v0 = new TestdataListEntityProvidingValue("v0");
        var v1 = new TestdataListEntityProvidingValue("v1");
        var v2 = new TestdataListEntityProvidingValue("v2");
        // A may hold all three; B may only hold v0 and v1 - not v2.
        // A list variable's backing list must be mutable,
        // so the value list arguments are wrapped,
        // unlike the fixed value range arguments.
        var entityA = new TestdataListEntityProvidingEntity("A", List.of(v0, v1, v2), new ArrayList<>(List.of(v0, v1, v2)));
        var entityB = new TestdataListEntityProvidingEntity("B", List.of(v0, v1), new ArrayList<>());
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(entityA, entityB));

        var context = NeighborhoodTester.build(new RelocateValueBlockMoveProvider(variableMetaModel), solutionMetaModel)
                .using(solution);

        // The block [v0, v1) is entirely within B's range,
        // so relocating it to B's only destination (index 0) is produced.
        context.producesAllOf(new RelocateValueBlockMove(variableMetaModel, entityA, 0, 2, entityB, 0));

        // Any block that includes v2 is not entirely within B's range, so it is never produced.
        context.producesNoneOf(new RelocateValueBlockMove(variableMetaModel, entityA, 1, 2, entityB, 0));
    }

}
