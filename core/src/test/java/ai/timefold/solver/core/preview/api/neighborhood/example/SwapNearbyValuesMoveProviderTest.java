package ai.timefold.solver.core.preview.api.neighborhood.example;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class SwapNearbyValuesMoveProviderTest {

    @Test
    void producesNearbySwapAndNoneAcrossADistanceOfTwo() {
        var solutionMetaModel = TestdataListEntityProvidingSolution.buildMetaModel();
        var variableMetaModel = solutionMetaModel.genuineEntity(TestdataListEntityProvidingEntity.class)
                .<TestdataListEntityProvidingValue> listVariable();

        var a0 = new TestdataListEntityProvidingValue("a0");
        var a1 = new TestdataListEntityProvidingValue("a1");
        var a2 = new TestdataListEntityProvidingValue("a2");
        var b0 = new TestdataListEntityProvidingValue("b0");
        var b1 = new TestdataListEntityProvidingValue("b1");
        var b2 = new TestdataListEntityProvidingValue("b2");
        // A may also hold b0; B may also hold a1 -
        // so swapping those two is in range both ways.
        // A list variable's backing list must be mutable,
        // so the value list arguments are wrapped.
        var entityA = new TestdataListEntityProvidingEntity("A", List.of(a0, a1, a2, b0),
                new ArrayList<>(List.of(a0, a1, a2)));
        var entityB = new TestdataListEntityProvidingEntity("B", List.of(b0, b1, b2, a1),
                new ArrayList<>(List.of(b0, b1, b2)));
        var solution = new TestdataListEntityProvidingSolution();
        solution.setEntityList(List.of(entityA, entityB));

        var context = NeighborhoodTester.build(new SwapNearbyValuesMoveProvider(variableMetaModel), solutionMetaModel)
                .using(solution);

        // a1 (A, index 1) and b0 (B, index 0) are one index apart,
        // different entities, and in range both ways.
        context.producesAllOf(Moves.swap(variableMetaModel, entityA, 1, entityB, 0));

        // a0 (A, index 0) and b2 (B, index 2) are two indexes apart -
        // structurally impossible regardless of value range, so it is never produced.
        context.producesNoneOf(Moves.swap(variableMetaModel, entityA, 0, entityB, 2));
    }

}
