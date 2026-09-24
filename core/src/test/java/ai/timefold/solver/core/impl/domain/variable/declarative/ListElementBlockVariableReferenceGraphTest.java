package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListUnassignMove;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Asserts that a change only recomputes the entities it can reach,
 * which is the point of {@link ListElementBlockVariableReferenceGraph}.
 */
class ListElementBlockVariableReferenceGraphTest {

    private static final int CHAIN_LENGTH = 6;
    private static final int VISITS_PER_VEHICLE = 3;
    private static final int LONG_VISIT_DURATION = 100;

    @Test
    void onlyReachableEntitiesAreRecomputed() {
        var solutionDescriptor = TestdataMultiEntityChainSolution.buildSolutionDescriptor();

        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainVehicle("B", 0);
        vehicleB.setPreviousVehicles(List.of(vehicleA));

        var a1 = new TestdataMultiEntityChainVisit("a1");
        var a2 = new TestdataMultiEntityChainVisit("a2");
        var a3 = new TestdataMultiEntityChainVisit("a3"); // Initially unassigned.
        var b1 = new TestdataMultiEntityChainVisit("b1");
        var b2 = new TestdataMultiEntityChainVisit("b2");
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1, b2)));

        var graphStructureAndDirection = GraphStructure.determineGraphStructure(solutionDescriptor,
                vehicleA, vehicleB, a1, a2, a3, b1, b2);
        assertThat(graphStructureAndDirection.blockedElementClass()).isEqualTo(TestdataMultiEntityChainVisit.class);

        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        var listVariableState = Mockito.mock(ListVariableState.class);
        Mockito.when(scoreDirector.getListVariableState(Mockito.any())).thenReturn(listVariableState);

        // The list variable listeners are not running, so the element shadow variables are set by hand.
        link(listVariableState, vehicleA, a1, null, 0);
        link(listVariableState, vehicleA, a2, a1, 1);
        link(listVariableState, vehicleB, b1, null, 0);
        link(listVariableState, vehicleB, b2, b1, 1);
        link(listVariableState, null, a3, null, -1);

        var graph = DefaultShadowVariableSessionFactory.buildListElementBlockGraph(
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(
                        solutionDescriptor, ChangedVariableNotifier.of(scoreDirector),
                        b2, vehicleB, a1, a3, vehicleA, b1, a2),
                graphStructureAndDirection);

        // The topological order puts vehicle A's block node before vehicle B's,
        // so every element is computed exactly once even at construction.
        assertThat(List.of(a1, a2, b1, b2)).allMatch(visit -> visit.getCalledCount() == 1);
        assertThat(a3.getCalledCount()).isOne();
        assertThat(vehicleB.getEndTime()).isEqualTo(4);

        vehicleA.reset();
        vehicleB.reset();
        List.of(a1, a2, a3, b1, b2).forEach(TestdataMultiEntityChainVisit::reset);

        // Append a3 to the end of vehicle A's route.
        vehicleA.getVisits().add(a3);
        link(listVariableState, vehicleA, a3, a2, 2);

        var visitMetaModel = solutionDescriptor.getMetaModel().entity(TestdataMultiEntityChainVisit.class);
        graph.afterVariableChanged(visitMetaModel.variable("vehicle"), a3);
        graph.afterVariableChanged(visitMetaModel.variable("previousVisit"), a3);
        graph.updateChanged();

        // The elements before the insertion point are unreachable from it and are left alone.
        assertThat(a1.getCalledCount()).isZero();
        assertThat(a2.getCalledCount()).isZero();
        // Pre-chain variables do not depend on the chain, so a chain-only change never recomputes them.
        assertThat(vehicleA.getPreviousEndTimeCalledCount()).isZero();
        // Everything downstream is recomputed exactly once:
        // the single pass in topological order visits vehicle A's block node,
        // its endTime, vehicle B's previousEndTime, vehicle B's block node
        // and finally vehicle B's endTime.
        assertThat(a3.getCalledCount()).isOne();
        assertThat(vehicleA.getEndTimeCalledCount()).isOne();
        assertThat(vehicleB.getPreviousEndTimeCalledCount()).isOne();
        assertThat(b1.getCalledCount()).isOne();
        assertThat(b2.getCalledCount()).isOne();
        assertThat(vehicleB.getEndTimeCalledCount()).isOne();
        assertThat(vehicleB.getEndTime()).isEqualTo(5);
    }

    @Test
    void deepChainRecomputesEachVariableOnce() {
        assertDeepChainRecomputesEachVariableOnce(false);
    }

    /**
     * A vehicle's endTime that also reads its own previousEndTime changes as soon as its predecessor's
     * endTime does. Its edge from the block node is what keeps it from being computed before the chain
     * it summarizes has been walked, and from running ahead of the walks down the vehicle chain.
     */
    @Test
    void deepChainRecomputesEachVariableOnceWhenTheEndTimeReadsThePreviousEndTime() {
        assertDeepChainRecomputesEachVariableOnce(true);
    }

    private static void assertDeepChainRecomputesEachVariableOnce(boolean endTimeIncludesPreviousEndTime) {
        var vehicleList = buildChain(endTimeIncludesPreviousEndTime);
        var unassignedVisit = new TestdataMultiEntityChainVisit("extra", LONG_VISIT_DURATION);
        var solution = buildSolution(vehicleList, unassignedVisit);

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);
        vehicleList.forEach(TestdataMultiEntityChainVehicle::reset);
        solution.getVisits().forEach(TestdataMultiEntityChainVisit::reset);

        // Appending a long visit to the head vehicle shifts every vehicle down the chain.
        var headVehicle = vehicleList.getFirst();
        context.execute(Moves.assign(listVariableMetaModel, unassignedVisit, headVehicle, VISITS_PER_VEHICLE));

        // The head vehicle's earlier visits are unreachable from the insertion point.
        assertThat(headVehicle.getVisits())
                .allSatisfy(visit -> assertThat(visit.getCalledCount()).isEqualTo(visit == unassignedVisit ? 1 : 0));
        // Pre-chain variables do not depend on the chain, so a chain-only change never recomputes them.
        assertThat(headVehicle.getPreviousEndTimeCalledCount()).isZero();
        assertThat(headVehicle.getEndTimeCalledCount()).isOne();
        for (var vehicle : vehicleList.subList(1, CHAIN_LENGTH)) {
            assertThat(vehicle.getVisits()).allSatisfy(visit -> assertThat(visit.getCalledCount()).isOne());
            assertThat(vehicle.getPreviousEndTimeCalledCount()).isOne();
            assertThat(vehicle.getEndTimeCalledCount()).isOne();
        }
        assertThat(vehicleList.getLast().getEndTime())
                .isEqualTo(LONG_VISIT_DURATION + CHAIN_LENGTH * VISITS_PER_VEHICLE);
    }

    @Test
    void buildingTheGraphDoesNotCompoundWalksAlongTheChain() {
        var vehicleList = buildChain(true);
        var solution = buildSolution(vehicleList, null);

        MoveTester.build(TestdataMultiEntityChainSolution.buildMetaModel()).using(solution);

        // Every entity is dirty when the graph is built, and each chain is walked exactly twice:
        // once by the graph's own bootstrap, and once by the from-scratch update
        // AbstractScoreDirector#setWorkingSolution forces on every graph.
        // Neither walk depends on how many vehicles precede the chain's vehicle,
        // which is what keeps building the graph linear in the length of a chain of vehicles.
        assertThat(solution.getVisits())
                .allSatisfy(visit -> assertThat(visit.getCalledCount()).isEqualTo(2));
        assertThat(vehicleList.getLast().getEndTime()).isEqualTo(CHAIN_LENGTH * VISITS_PER_VEHICLE);
    }

    /**
     * Like the single directional parent graph, each walk starts from an element whose sources changed
     * and stops at the first element that is unchanged, so an element between two walks is left alone.
     */
    @Test
    void swapWalksFromEachChangedElementUntilOneIsUnchanged() {
        var v0 = new TestdataMultiEntityChainVisit("v0");
        var v1 = new TestdataMultiEntityChainVisit("v1");
        var v2 = new TestdataMultiEntityChainVisit("v2");
        var v3 = new TestdataMultiEntityChainVisit("v3");
        var v4 = new TestdataMultiEntityChainVisit("v4");
        var vehicle = new TestdataMultiEntityChainVehicle("A", 0);
        vehicle.setVisits(new ArrayList<>(List.of(v0, v1, v2, v3, v4)));
        var solution = buildSolution(List.of(vehicle), null);
        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);
        solution.getVisits().forEach(TestdataMultiEntityChainVisit::reset);

        // All durations are equal, so only the swapped visits change their end time.
        // Every visit but v2 changes its previous visit.
        context.execute(Moves.swap(listVariableMetaModel, vehicle, 0, vehicle, 3));
        assertThat(vehicle.getVisits()).containsExactly(v3, v1, v2, v0, v4);
        assertThat(v3.getEndServiceTime()).isOne();
        assertThat(v0.getEndServiceTime()).isEqualTo(4);
        // One walk from v3 stops at v1, unchanged; another from v0 stops at v4, unchanged.
        assertThat(List.of(v3, v1, v0, v4)).allSatisfy(visit -> assertThat(visit.getCalledCount()).isOne());
        assertThat(v2.getCalledCount()).isZero();
    }

    /**
     * A forced update changes nothing, so no element records itself;
     * still, every element is recomputed, so that a corrupted one is caught.
     */
    @Test
    void forcedUpdateRecomputesEveryElementOnce() {
        var vehicleList = buildChain(false);
        var solution = buildSolution(vehicleList, null);
        var scoreDirector =
                PlannerTestUtils.mockScoreDirector(TestdataMultiEntityChainSolution.buildSolutionDescriptor());
        scoreDirector.setWorkingSolution(solution);
        var visit = vehicleList.getFirst().getVisits().get(1);
        var endServiceTime = visit.getEndServiceTime();
        visit.setEndServiceTime(-1);
        solution.getVisits().forEach(TestdataMultiEntityChainVisit::reset);

        scoreDirector.forceUpdateShadowVariables();
        assertThat(visit.getEndServiceTime()).isEqualTo(endServiceTime);
        assertThat(solution.getVisits()).allSatisfy(v -> assertThat(v.getCalledCount()).isOne());
    }

    @Test
    void unassigningAnElementComputesItOnce() {
        var vehicleList = buildChain(false);
        var solution = buildSolution(vehicleList, null);
        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);
        var vehicle = vehicleList.getFirst();
        var visit = vehicle.getVisits().get(1);
        solution.getVisits().forEach(TestdataMultiEntityChainVisit::reset);

        // Its vehicle and previous visit both change, one event each.
        context.execute(Moves.unassign(listVariableMetaModel, vehicle, 1));
        assertThat(visit.getEndServiceTime()).isNull();
        assertThat(visit.getCalledCount()).isOne();
    }

    @Test
    void anElementChangedThenUnassignedInOneUpdateIsComputedOnce() {
        var vehicleList = buildChain(false);
        var solution = buildSolution(vehicleList, null);
        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataMultiEntityChainSolution, TestdataMultiEntityChainVehicle, TestdataMultiEntityChainVisit>) solutionMetaModel
                        .genuineEntity(TestdataMultiEntityChainVehicle.class)
                        .listVariable("visits", TestdataMultiEntityChainVisit.class))
                        .variableDescriptor();
        var context = MoveTester.build(solutionMetaModel).using(solution);
        var vehicle = vehicleList.getFirst();
        var visit = vehicle.getVisits().get(1);
        solution.getVisits().forEach(TestdataMultiEntityChainVisit::reset);

        // In one update: the visit's predecessor moves to another vehicle, then the visit is unassigned.
        context.execute(SelectorBasedCompositeMove.buildMove(
                new SelectorBasedListChangeMove<>(listVariableDescriptor, vehicle, 0, vehicleList.getLast(), 0),
                new SelectorBasedListUnassignMove<>(listVariableDescriptor, vehicle, 0)));
        assertThat(visit.getEndServiceTime()).isNull();
        assertThat(visit.getCalledCount()).isOne();
    }

    private static List<TestdataMultiEntityChainVehicle> buildChain(boolean endTimeIncludesPreviousEndTime) {
        var vehicleList = new ArrayList<TestdataMultiEntityChainVehicle>(CHAIN_LENGTH);
        for (var vehicleIndex = 0; vehicleIndex < CHAIN_LENGTH; vehicleIndex++) {
            var vehicle = new TestdataMultiEntityChainVehicle("V" + vehicleIndex, 0);
            vehicle.setEndTimeIncludesPreviousEndTime(endTimeIncludesPreviousEndTime);
            if (vehicleIndex > 0) {
                vehicle.setPreviousVehicles(List.of(vehicleList.get(vehicleIndex - 1)));
            }
            var visitList = new ArrayList<TestdataMultiEntityChainVisit>(VISITS_PER_VEHICLE);
            for (var visitIndex = 0; visitIndex < VISITS_PER_VEHICLE; visitIndex++) {
                visitList.add(new TestdataMultiEntityChainVisit("v%d_%d".formatted(vehicleIndex, visitIndex)));
            }
            vehicle.setVisits(visitList);
            vehicleList.add(vehicle);
        }
        return vehicleList;
    }

    private static TestdataMultiEntityChainSolution buildSolution(List<TestdataMultiEntityChainVehicle> vehicleList,
            @Nullable TestdataMultiEntityChainVisit unassignedVisit) {
        var visitList = new ArrayList<TestdataMultiEntityChainVisit>();
        vehicleList.forEach(vehicle -> visitList.addAll(vehicle.getVisits()));
        if (unassignedVisit != null) {
            visitList.add(unassignedVisit);
        }
        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(vehicleList);
        solution.setVisits(visitList);
        return solution;
    }

    private static void link(
            ListVariableState<TestdataMultiEntityChainSolution, TestdataMultiEntityChainVehicle, TestdataMultiEntityChainVisit> listVariableState,
            TestdataMultiEntityChainVehicle vehicle, TestdataMultiEntityChainVisit visit,
            TestdataMultiEntityChainVisit previousVisit, int index) {
        visit.setVehicle(vehicle);
        visit.setPreviousVisit(previousVisit);
        Mockito.doReturn(index).when(listVariableState).getIndexOrFail(visit);
        Mockito.when(listVariableState.getInverseSingleton(visit)).thenReturn(vehicle);
    }
}
