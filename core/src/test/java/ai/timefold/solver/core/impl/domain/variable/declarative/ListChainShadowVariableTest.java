package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.assertMatchesArbitraryGraph;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListChainVariableReferenceGraph} on a model where
 * a vehicle starts where its predecessor vehicles end.
 */
class ListChainShadowVariableTest {

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
        // v1 does not read it and stays unchanged, but v2 re-bases on it.
        context.execute(Moves.assign(listVariableMetaModel, w, vehicleA, 0));
        assertThat(vehicleA.getEndTime()).isEqualTo(5);
        assertThat(vehicleB.getPreviousEndTime()).isEqualTo(5);
        assertThat(v1.getEndServiceTime()).isEqualTo(1);
        assertThat(v2.getEndServiceTime()).isEqualTo(6);
        assertThat(v3.getEndServiceTime()).isEqualTo(7);
        assertThat(vehicleB.getEndTime()).isEqualTo(7);
        assertShadowsAreAtFixedPoint(solution);
    }

    /**
     * Emptying a route leaves no element to walk, so only the entity's post-chain variables carry
     * the change; the list change is what marks them.
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
     * The list change records the visits it moves, from which the chain node would walk the route;
     * the pre-chain change in the same update, B's later departure raising its previousEndTime,
     * is what makes it walk from the head of the route instead.
     */
    @Test
    void preChainAndListChangeInOneUpdateWalkTheWholeRoute() {
        var a1 = new TestdataMultiEntityChainVisit("a1", 2);
        var b1 = new TestdataMultiEntityChainVisit("b1", 2);
        var b2 = new TestdataMultiEntityChainVisit("b2", 3);
        var b3 = new TestdataMultiEntityChainVisit("b3", 4);

        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleB.setPreviousVehicles(List.of(vehicleA));
        vehicleA.setVisits(new ArrayList<>(List.of(a1)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1, b2, b3)));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, b1, b2, b3));

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class);
        var departureTimeDescriptor =
                ((DefaultPlanningVariableMetaModel<TestdataMultiEntityChainSolution, TestdataMultiEntityChainVehicle, Integer>) vehicleMetaModel
                        .basicVariable("departureTime", Integer.class))
                        .variableDescriptor();
        var listVariableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataMultiEntityChainSolution, TestdataMultiEntityChainVehicle, TestdataMultiEntityChainVisit>) vehicleMetaModel
                        .listVariable("visits", TestdataMultiEntityChainVisit.class))
                        .variableDescriptor();
        var context = MoveTester.build(solutionMetaModel).using(solution);

        // In one update: B departs later, and b3 moves between b1 and b2.
        context.execute(SelectorBasedCompositeMove.buildMove(
                new SelectorBasedChangeMove<>(departureTimeDescriptor, vehicleB, 10),
                new SelectorBasedListChangeMove<>(listVariableDescriptor, vehicleB, 2, vehicleB, 1)));
        assertThat(vehicleB.getVisits()).containsExactly(b1, b3, b2);
        assertThat(b1.getEndServiceTime()).isEqualTo(12);
        assertThat(b3.getEndServiceTime()).isEqualTo(16);
        assertThat(b2.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicleB.getEndTime()).isEqualTo(19);
        assertShadowsAreAtFixedPoint(solution);
    }

    @Test
    void solutionWithoutVehiclesFallsBack() {
        var visit = new TestdataMultiEntityChainVisit("v1");

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of());
        solution.setVisits(List.of(visit));

        // Without a list entity there is no chain node, so the arbitrary graph covers the unassigned visit.
        MoveTester.build(TestdataMultiEntityChainSolution.buildMetaModel()).using(solution);
        assertThat(visit.getEndServiceTime()).isNull();
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

    private static void assertShadowsAreAtFixedPoint(TestdataMultiEntityChainSolution solution) {
        assertMatchesArbitraryGraph(TestdataMultiEntityChainSolution.buildSolutionDescriptor(), solution,
                "Shadow variables");
    }
}
