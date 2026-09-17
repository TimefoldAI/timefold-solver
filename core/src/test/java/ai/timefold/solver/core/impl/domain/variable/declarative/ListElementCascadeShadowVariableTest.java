package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.solveWithFullAssert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListElementCascadeVariableReferenceGraph} on a model where
 * a vehicle starts where its predecessor vehicles end.
 */
class ListElementCascadeShadowVariableTest {

    @Test
    void changeOnPredecessorVehiclePropagates() {
        var x1 = new TestdataMultiEntityChainVisit("x1");
        var x2 = new TestdataMultiEntityChainVisit("x2");
        var x3 = new TestdataMultiEntityChainVisit("x3"); // Initially unassigned.
        var y1 = new TestdataMultiEntityChainVisit("y1");
        var y2 = new TestdataMultiEntityChainVisit("y2");

        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleB.setPreviousVehicles(List.of(vehicleA));
        vehicleA.setVisits(new ArrayList<>(List.of(x1, x2)));
        vehicleB.setVisits(new ArrayList<>(List.of(y1, y2)));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(x1, x2, x3, y1, y2));

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        // A = [1, 2], endTime 2; B starts at 2 -> [3, 4], endTime 4.
        assertThat(vehicleA.getEndTime()).isEqualTo(2);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(2);
        assertThat(y2.getEndServiceTime()).isEqualTo(4);
        assertThat(vehicleB.getEndTime()).isEqualTo(4);

        // Appending x3 to A shifts B's whole route.
        context.execute(Moves.assign(listVariableMetaModel, x3, vehicleA, 2));
        assertThat(x3.getEndServiceTime()).isEqualTo(3);
        assertThat(vehicleA.getEndTime()).isEqualTo(3);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(3);
        assertThat(y1.getEndServiceTime()).isEqualTo(4);
        assertThat(y2.getEndServiceTime()).isEqualTo(5);
        assertThat(vehicleB.getEndTime()).isEqualTo(5);
        assertShadowsAreAtFixedPoint(solution);

        // Moving x1 (head of A) to B rechains both vehicles.
        context.execute(Moves.change(listVariableMetaModel, vehicleA, 0, vehicleB, 0));
        assertThat(vehicleA.getEndTime()).isEqualTo(2);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(2);
        assertThat(x1.getEndServiceTime()).isEqualTo(3);
        assertThat(y2.getEndServiceTime()).isEqualTo(5);
        assertThat(vehicleB.getEndTime()).isEqualTo(5);
        assertShadowsAreAtFixedPoint(solution);
    }

    @Test
    void swapCreatesNonContiguousDirtyElements() {
        var v1 = new TestdataMultiEntityChainVisit("v1", 1);
        var v2 = new TestdataMultiEntityChainVisit("v2", 5);
        var v3 = new TestdataMultiEntityChainVisit("v3", 3);
        var v4 = new TestdataMultiEntityChainVisit("v4", 2);
        var vehicle = new TestdataMultiEntityChainVehicle("A", 0);
        vehicle.setVisits(new ArrayList<>(List.of(v1, v2, v3, v4)));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicle));
        solution.setVisits(List.of(v1, v2, v3, v4));

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(vehicle.getEndTime()).isEqualTo(1 + 5 + 3 + 2);

        // Swap the first and third visits: the dirty elements are non-contiguous.
        context.execute(Moves.swap(listVariableMetaModel, vehicle, 0, vehicle, 2));
        assertThat(v3.getEndServiceTime()).isEqualTo(3);
        assertThat(v2.getEndServiceTime()).isEqualTo(8);
        assertThat(v1.getEndServiceTime()).isEqualTo(9);
        assertThat(v4.getEndServiceTime()).isEqualTo(11);
        assertThat(vehicle.getEndTime()).isEqualTo(11);
        assertShadowsAreAtFixedPoint(solution);
    }

    /**
     * Emptying a route leaves no element to walk, so only the owner's post-chain variables
     * carry the change; the cascade must mark them even though the changed range is empty.
     */
    @Test
    void emptyingARouteUpdatesItsPostChainVariables() {
        var x1 = new TestdataMultiEntityChainVisit("x1", 2);
        var y1 = new TestdataMultiEntityChainVisit("y1", 3);

        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleB.setPreviousVehicles(List.of(vehicleA));
        vehicleA.setVisits(new ArrayList<>(List.of(x1)));
        vehicleB.setVisits(new ArrayList<>(List.of(y1)));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(x1, y1));

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        // A = [2], endTime 2; B starts at 2 -> [5], endTime 5.
        assertThat(vehicleA.getEndTime()).isEqualTo(2);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(2);
        assertThat(y1.getEndServiceTime()).isEqualTo(5);

        // Unassigning A's only visit falls its endTime back to its departure time,
        // which shifts B's whole route even though A has no element left to walk.
        context.execute(Moves.unassign(listVariableMetaModel, vehicleA, 0));
        assertThat(x1.getEndServiceTime()).isNull();
        assertThat(vehicleA.getEndTime()).isZero();
        assertThat(vehicleB.getPreviousEndTime()).isZero();
        assertThat(y1.getEndServiceTime()).isEqualTo(3);
        assertThat(vehicleB.getEndTime()).isEqualTo(3);
        assertShadowsAreAtFixedPoint(solution);
    }

    /**
     * An element in the middle of the chain reads a pre-chain variable directly,
     * so a pre-chain change must reach it even when its predecessors are unchanged.
     */
    @Test
    void preChainChangeReachesElementReadingIt() {
        var w = new TestdataMultiEntityChainVisit("w", 5, false); // Initially unassigned.
        var v1 = new TestdataMultiEntityChainVisit("v1", 1, false);
        var v2 = new TestdataMultiEntityChainVisit("v2", 1, true);
        var v3 = new TestdataMultiEntityChainVisit("v3", 1, false);

        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleB.setPreviousVehicles(List.of(vehicleA));
        vehicleB.setVisits(new ArrayList<>(List.of(v1, v2, v3)));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(w, v1, v2, v3));

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(0);
        assertThat(v1.getEndServiceTime()).isEqualTo(1);
        assertThat(v2.getEndServiceTime()).isEqualTo(2);
        assertThat(v3.getEndServiceTime()).isEqualTo(3);
        assertThat(vehicleB.getEndTime()).isEqualTo(3);

        // Assigning w to A changes B's previousEndTime;
        // v1 does not read it and stays unchanged, but v2 reads it directly.
        context.execute(Moves.assign(listVariableMetaModel, w, vehicleA, 0));
        assertThat(vehicleA.getEndTime()).isEqualTo(5);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(5);
        assertThat(v1.getEndServiceTime()).isEqualTo(1);
        assertThat(v2.getEndServiceTime()).isEqualTo(6);
        assertThat(v3.getEndServiceTime()).isEqualTo(7);
        assertThat(vehicleB.getEndTime()).isEqualTo(7);
        assertShadowsAreAtFixedPoint(solution);
    }

    @Test
    void cyclicVehicleFactsFailFast() {
        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleA.setPreviousVehicles(List.of(vehicleB));
        vehicleB.setPreviousVehicles(List.of(vehicleA));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(new TestdataMultiEntityChainVisit("v1")));

        assertThatCode(() -> MoveTester.build(TestdataMultiEntityChainSolution.buildMetaModel()).using(solution))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixed dependency loops");
    }

    @Test
    void solveWithFullAssertFromUninitializedSolution() {
        assertShadowsAreAtFixedPoint(solve(generateSolution(false)));
    }

    @Test
    void solveWithFullAssertWithPreChainReadingElements() {
        assertShadowsAreAtFixedPoint(solve(generateSolution(true)));
    }

    private static TestdataMultiEntityChainSolution solve(TestdataMultiEntityChainSolution problem) {
        return solveWithFullAssert(TestdataMultiEntityChainSolution.class,
                TestdataMultiEntityChainConstraintProvider.class, problem,
                TestdataMultiEntityChainVehicle.class, TestdataMultiEntityChainVisit.class);
    }

    private static TestdataMultiEntityChainSolution generateSolution(boolean alternatePreChainReaders) {
        var vehicles = new ArrayList<TestdataMultiEntityChainVehicle>();
        for (var i = 0; i < 3; i++) {
            vehicles.add(new TestdataMultiEntityChainVehicle("vehicle" + i, i));
        }
        // vehicle0 -> vehicle1 -> vehicle2 chain.
        vehicles.get(1).setPreviousVehicles(List.of(vehicles.get(0)));
        vehicles.get(2).setPreviousVehicles(List.of(vehicles.get(1)));
        var visits = new ArrayList<TestdataMultiEntityChainVisit>();
        for (var i = 0; i < 6; i++) {
            visits.add(new TestdataMultiEntityChainVisit("visit" + i, 1 + (i % 3),
                    !alternatePreChainReaders || i % 2 == 0));
        }
        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(vehicles);
        solution.setVisits(visits);
        return solution;
    }

    private static void assertShadowsAreAtFixedPoint(TestdataMultiEntityChainSolution solution) {
        DeclarativeShadowVariableAssertions.assertShadowsAreAtFixedPoint(solution,
                s -> s.getVehicles().stream().map(TestdataMultiEntityChainVehicle::getEndTime).toList(),
                s -> s.getVisits().stream().map(TestdataMultiEntityChainVisit::getEndServiceTime).toList());
    }
}
