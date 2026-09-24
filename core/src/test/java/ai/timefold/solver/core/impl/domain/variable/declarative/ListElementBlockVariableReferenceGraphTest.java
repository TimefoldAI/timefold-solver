package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;

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
        var listStateSupply = Mockito.mock(ListVariableStateSupply.class);
        Mockito.when(scoreDirector.getListVariableStateSupply(Mockito.any())).thenReturn(listStateSupply);

        // The list variable listeners are not running, so the element shadow variables are set by hand.
        link(listStateSupply, vehicleA, a1, null, a2, 0);
        link(listStateSupply, vehicleA, a2, a1, null, 1);
        link(listStateSupply, vehicleB, b1, null, b2, 0);
        link(listStateSupply, vehicleB, b2, b1, null, 1);
        link(listStateSupply, null, a3, null, null, -1);

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
        link(listStateSupply, vehicleA, a3, a2, null, 2);
        Mockito.when(listStateSupply.getNextElement(a2)).thenReturn(a3);

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
            ListVariableStateSupply<TestdataMultiEntityChainSolution, TestdataMultiEntityChainVehicle, TestdataMultiEntityChainVisit> listStateSupply,
            TestdataMultiEntityChainVehicle vehicle, TestdataMultiEntityChainVisit visit,
            TestdataMultiEntityChainVisit previousVisit, TestdataMultiEntityChainVisit nextVisit, int index) {
        visit.setVehicle(vehicle);
        visit.setPreviousVisit(previousVisit);
        Mockito.doReturn(index).when(listStateSupply).getIndexOrElse(Mockito.eq(visit), Mockito.anyInt());
        Mockito.when(listStateSupply.getNextElement(visit)).thenReturn(nextVisit);
        Mockito.when(listStateSupply.getInverseSingleton(visit)).thenReturn(vehicle);
    }
}
