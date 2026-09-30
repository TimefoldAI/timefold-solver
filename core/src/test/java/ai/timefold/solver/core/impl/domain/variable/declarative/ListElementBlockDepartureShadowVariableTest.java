package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.executeRandomListMove;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure.TestdataMultiEntityChainDepartureSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure.TestdataMultiEntityChainDepartureVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure.TestdataMultiEntityChainDepartureVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListElementBlockVariableReferenceGraph} on a model where the first visit of a route
 * reads its vehicle's departure time, a planning variable.
 * Unlike a declarative variable of the vehicle, it has no graph node to reach the block node through,
 * so its change marks the block node directly.
 */
class ListElementBlockDepartureShadowVariableTest {

    @Test
    void departureTimeChangeShiftsTheWholeRoute() {
        var a1 = new TestdataMultiEntityChainDepartureVisit("a1", 2);
        var a2 = new TestdataMultiEntityChainDepartureVisit("a2", 3);
        var a3 = new TestdataMultiEntityChainDepartureVisit("a3", 4);
        var vehicle = new TestdataMultiEntityChainDepartureVehicle("A", 0);
        vehicle.setVisits(new ArrayList<>(List.of(a1, a2, a3)));
        var solution = buildSolution(List.of(vehicle), List.of(a1, a2, a3));

        var solutionMetaModel = TestdataMultiEntityChainDepartureSolution.buildMetaModel();
        var departureTimeMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainDepartureVehicle.class)
                .basicVariable("departureTime", Integer.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);
        // Departs at 0 -> [2, 5, 9].
        assertThat(a3.getEndServiceTime()).isEqualTo(9);
        assertThat(vehicle.getEndTime()).isEqualTo(9);

        // Only the first visit reads the departure time; the rest of the route follows it.
        context.execute(Moves.change(departureTimeMetaModel, vehicle, 10));
        assertThat(a1.getEndServiceTime()).isEqualTo(12);
        assertThat(a2.getEndServiceTime()).isEqualTo(15);
        assertThat(a3.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicle.getEndTime()).isEqualTo(19);
        assertShadowsAreAtFixedPoint(solution);
    }

    /**
     * The list change records the visits it moves, from which the block node would walk the route;
     * the departure time change in the same update is what makes it walk from the head of the route instead.
     */
    @Test
    void departureTimeAndListChangeInOneUpdateWalkTheWholeRoute() {
        var a1 = new TestdataMultiEntityChainDepartureVisit("a1", 2);
        var a2 = new TestdataMultiEntityChainDepartureVisit("a2", 3);
        var a3 = new TestdataMultiEntityChainDepartureVisit("a3", 4);
        var vehicle = new TestdataMultiEntityChainDepartureVehicle("A", 0);
        vehicle.setVisits(new ArrayList<>(List.of(a1, a2, a3)));
        var solution = buildSolution(List.of(vehicle), List.of(a1, a2, a3));

        var solutionMetaModel = TestdataMultiEntityChainDepartureSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainDepartureVehicle.class);
        var departureTimeDescriptor =
                ((DefaultPlanningVariableMetaModel<TestdataMultiEntityChainDepartureSolution, TestdataMultiEntityChainDepartureVehicle, Integer>) vehicleMetaModel
                        .basicVariable("departureTime", Integer.class))
                        .variableDescriptor();
        var listVariableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataMultiEntityChainDepartureSolution, TestdataMultiEntityChainDepartureVehicle, TestdataMultiEntityChainDepartureVisit>) vehicleMetaModel
                        .listVariable("visits", TestdataMultiEntityChainDepartureVisit.class))
                        .variableDescriptor();
        var context = MoveTester.build(solutionMetaModel).using(solution);

        // In one update: the vehicle departs later, and a3 moves between a1 and a2.
        context.execute(SelectorBasedCompositeMove.buildMove(
                new SelectorBasedChangeMove<>(departureTimeDescriptor, vehicle, 10),
                new SelectorBasedListChangeMove<>(listVariableDescriptor, vehicle, 2, vehicle, 1)));
        assertThat(vehicle.getVisits()).containsExactly(a1, a3, a2);
        assertThat(a1.getEndServiceTime()).isEqualTo(12);
        assertThat(a3.getEndServiceTime()).isEqualTo(16);
        assertThat(a2.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicle.getEndTime()).isEqualTo(19);
        assertShadowsAreAtFixedPoint(solution);
    }

    /**
     * A departure time change walks the whole route, so no walk from a changed visit has to reach
     * the visit reading it, and each walk still stops at the first unchanged visit.
     */
    @Test
    void swapWalksFromEachChangedVisitUntilOneIsUnchanged() {
        var v0 = new TestdataMultiEntityChainDepartureVisit("v0", 1);
        var v1 = new TestdataMultiEntityChainDepartureVisit("v1", 1);
        var v2 = new TestdataMultiEntityChainDepartureVisit("v2", 1);
        var v3 = new TestdataMultiEntityChainDepartureVisit("v3", 1);
        var v4 = new TestdataMultiEntityChainDepartureVisit("v4", 1);
        var vehicle = new TestdataMultiEntityChainDepartureVehicle("A", 0);
        vehicle.setVisits(new ArrayList<>(List.of(v0, v1, v2, v3, v4)));
        var solution = buildSolution(List.of(vehicle), List.of(v0, v1, v2, v3, v4));

        var solutionMetaModel = TestdataMultiEntityChainDepartureSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainDepartureVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainDepartureVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);
        solution.getVisits().forEach(TestdataMultiEntityChainDepartureVisit::reset);

        // All durations are equal, so only the swapped visits change their end time.
        context.execute(Moves.swap(listVariableMetaModel, vehicle, 0, vehicle, 3));
        assertThat(vehicle.getVisits()).containsExactly(v3, v1, v2, v0, v4);
        // One walk from v3 stops at v1, unchanged; another from v0 stops at v4, unchanged.
        assertThat(List.of(v3, v1, v0, v4)).allSatisfy(visit -> assertThat(visit.getCalledCount()).isOne());
        assertThat(v2.getCalledCount()).isZero();
    }

    /**
     * Differential test: after every random move, the incrementally maintained shadow
     * variables must equal a from-scratch recomputation, which uses the arbitrary graph.
     */
    @Test
    void randomMovesStayAtFixedPoint() {
        for (var seed = 0; seed < 30; seed++) {
            var random = new Random(seed);
            var solution = generateSolution();
            var solutionMetaModel = TestdataMultiEntityChainDepartureSolution.buildMetaModel();
            var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainDepartureVehicle.class);
            var departureTimeMetaModel = vehicleMetaModel.basicVariable("departureTime", Integer.class);
            var listVariableMetaModel = vehicleMetaModel.listVariable("visits", TestdataMultiEntityChainDepartureVisit.class);
            var context = MoveTester.build(solutionMetaModel).using(solution);
            var vehicleList = solution.getVehicles();
            var departureTimeList = solution.getDepartureTimes();
            for (var moveIndex = 0; moveIndex < 40; moveIndex++) {
                if (random.nextInt(4) == 0) {
                    context.execute(Moves.change(departureTimeMetaModel, vehicleList.get(random.nextInt(vehicleList.size())),
                            departureTimeList.get(random.nextInt(departureTimeList.size()))));
                } else {
                    executeRandomListMove(context, listVariableMetaModel, TestdataMultiEntityChainDepartureVehicle::getVisits,
                            vehicleList, solution.getVisits(), random);
                }
                assertShadowsAreAtFixedPoint(solution);
            }
        }
    }

    private static TestdataMultiEntityChainDepartureSolution generateSolution() {
        var vehicles = new ArrayList<TestdataMultiEntityChainDepartureVehicle>();
        for (var i = 0; i < 3; i++) {
            vehicles.add(new TestdataMultiEntityChainDepartureVehicle("vehicle" + i, 10 * i));
        }
        var visits = new ArrayList<TestdataMultiEntityChainDepartureVisit>();
        for (var i = 0; i < 6; i++) {
            visits.add(new TestdataMultiEntityChainDepartureVisit("visit" + i, 1 + (i % 3)));
        }
        return buildSolution(vehicles, visits);
    }

    private static TestdataMultiEntityChainDepartureSolution buildSolution(
            List<TestdataMultiEntityChainDepartureVehicle> vehicleList,
            List<TestdataMultiEntityChainDepartureVisit> visitList) {
        var solution = new TestdataMultiEntityChainDepartureSolution();
        solution.setDepartureTimes(List.of(0, 5, 10, 20));
        solution.setVehicles(vehicleList);
        solution.setVisits(visitList);
        return solution;
    }

    private static void assertShadowsAreAtFixedPoint(TestdataMultiEntityChainDepartureSolution solution) {
        DeclarativeShadowVariableAssertions.assertShadowsAreAtFixedPoint(solution,
                s -> s.getVehicles().stream().map(TestdataMultiEntityChainDepartureVehicle::getEndTime).toList(),
                s -> s.getVisits().stream().map(TestdataMultiEntityChainDepartureVisit::getEndServiceTime).toList());
    }
}
