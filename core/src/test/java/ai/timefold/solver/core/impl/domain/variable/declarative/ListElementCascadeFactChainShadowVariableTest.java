package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.solveWithFullAssert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact.TestdataFactChainConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact.TestdataFactChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact.TestdataFactChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact.TestdataFactChainVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListElementCascadeVariableReferenceGraph} on a model where
 * vehicles are chained to each other by a plain fact instead of a fact collection.
 */
class ListElementCascadeFactChainShadowVariableTest {

    @Test
    void changeOnPredecessorVehiclePropagatesThroughFactChain() {
        var x1 = new TestdataFactChainVisit("x1");
        var x2 = new TestdataFactChainVisit("x2");
        var x3 = new TestdataFactChainVisit("x3"); // Initially unassigned.
        var y1 = new TestdataFactChainVisit("y1");
        var z1 = new TestdataFactChainVisit("z1");

        var vehicleA = new TestdataFactChainVehicle("A", 0);
        var vehicleB = new TestdataFactChainVehicle("B", 0);
        var vehicleC = new TestdataFactChainVehicle("C", 1);
        vehicleB.setPreviousVehicle(vehicleA);
        vehicleC.setPreviousVehicle(vehicleB);
        vehicleA.setVisits(new ArrayList<>(List.of(x1, x2)));
        vehicleB.setVisits(new ArrayList<>(List.of(y1)));
        vehicleC.setVisits(new ArrayList<>(List.of(z1)));

        var solution = new TestdataFactChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB, vehicleC));
        solution.setVisits(List.of(x1, x2, x3, y1, z1));

        var solutionMetaModel = TestdataFactChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataFactChainVehicle.class)
                .listVariable("visits", TestdataFactChainVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        // A = [1, 2]; B starts at 2 -> [3]; C starts at 3 -> [4].
        assertThat(vehicleA.getEndTime()).isEqualTo(2);
        assertThat(vehicleB.getStartTime()).isEqualTo(2);
        assertThat(vehicleB.getEndTime()).isEqualTo(3);
        assertThat(vehicleC.getStartTime()).isEqualTo(3);
        assertThat(z1.getEndServiceTime()).isEqualTo(4);

        // Appending x3 to A shifts both B's and C's whole routes.
        context.execute(Moves.assign(listVariableMetaModel, x3, vehicleA, 2));
        assertThat(x3.getEndServiceTime()).isEqualTo(3);
        assertThat(vehicleA.getEndTime()).isEqualTo(3);
        assertThat(vehicleB.getStartTime()).isEqualTo(3);
        assertThat(y1.getEndServiceTime()).isEqualTo(4);
        assertThat(vehicleC.getStartTime()).isEqualTo(4);
        assertThat(z1.getEndServiceTime()).isEqualTo(5);
        assertThat(vehicleC.getEndTime()).isEqualTo(5);
        assertShadowsAreAtFixedPoint(solution);

        // Moving the head of A to C rechains all three vehicles.
        context.execute(Moves.change(listVariableMetaModel, vehicleA, 0, vehicleC, 0));
        assertThat(vehicleA.getEndTime()).isEqualTo(2);
        assertThat(vehicleB.getStartTime()).isEqualTo(2);
        assertThat(y1.getEndServiceTime()).isEqualTo(3);
        assertThat(vehicleC.getStartTime()).isEqualTo(3);
        assertThat(x1.getEndServiceTime()).isEqualTo(4);
        assertThat(z1.getEndServiceTime()).isEqualTo(5);
        assertThat(vehicleC.getEndTime()).isEqualTo(5);
        assertShadowsAreAtFixedPoint(solution);
    }

    @Test
    void cyclicVehicleFactsFailFast() {
        var vehicleA = new TestdataFactChainVehicle("A", 0);
        var vehicleB = new TestdataFactChainVehicle("B", 0);
        vehicleA.setPreviousVehicle(vehicleB);
        vehicleB.setPreviousVehicle(vehicleA);

        var solution = new TestdataFactChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(new TestdataFactChainVisit("v1")));

        assertThatCode(() -> MoveTester.build(TestdataFactChainSolution.buildMetaModel()).using(solution))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fixed dependency loops");
    }

    @Test
    void solveWithFullAssertFromUninitializedSolution() {
        assertShadowsAreAtFixedPoint(solve(generateSolution(false)));
    }

    @Test
    void solveWithFullAssertFromInitializedSolution() {
        assertShadowsAreAtFixedPoint(solve(generateSolution(true)));
    }

    private static TestdataFactChainSolution solve(TestdataFactChainSolution problem) {
        return solveWithFullAssert(TestdataFactChainSolution.class,
                TestdataFactChainConstraintProvider.class, problem,
                TestdataFactChainVehicle.class, TestdataFactChainVisit.class);
    }

    private static TestdataFactChainSolution generateSolution(boolean initialized) {
        var vehicles = new ArrayList<TestdataFactChainVehicle>();
        for (var i = 0; i < 3; i++) {
            vehicles.add(new TestdataFactChainVehicle("vehicle" + i, i));
        }
        // vehicle0 -> vehicle1 -> vehicle2 chain.
        vehicles.get(1).setPreviousVehicle(vehicles.get(0));
        vehicles.get(2).setPreviousVehicle(vehicles.get(1));
        var visits = new ArrayList<TestdataFactChainVisit>();
        for (var i = 0; i < 6; i++) {
            visits.add(new TestdataFactChainVisit("visit" + i, 1 + (i % 3)));
        }
        if (initialized) {
            vehicles.get(0).setVisits(new ArrayList<>(visits.subList(0, 4)));
            vehicles.get(1).setVisits(new ArrayList<>(visits.subList(4, 6)));
        }
        var solution = new TestdataFactChainSolution();
        solution.setVehicles(vehicles);
        solution.setVisits(visits);
        return solution;
    }

    private static void assertShadowsAreAtFixedPoint(TestdataFactChainSolution solution) {
        DeclarativeShadowVariableAssertions.assertShadowsAreAtFixedPoint(solution,
                s -> s.getVehicles().stream().map(TestdataFactChainVehicle::getStartTime).toList(),
                s -> s.getVehicles().stream().map(TestdataFactChainVehicle::getEndTime).toList(),
                s -> s.getVisits().stream().map(TestdataFactChainVisit::getEndServiceTime).toList());
    }
}
