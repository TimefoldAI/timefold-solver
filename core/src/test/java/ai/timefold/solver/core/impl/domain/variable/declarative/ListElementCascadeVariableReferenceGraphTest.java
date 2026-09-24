package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Asserts that a change only recomputes the entities it can reach,
 * which is the point of {@link ListElementCascadeVariableReferenceGraph}.
 */
class ListElementCascadeVariableReferenceGraphTest {

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
        assertThat(graphStructureAndDirection.cascadedElementClass()).isEqualTo(TestdataMultiEntityChainVisit.class);

        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        var listStateSupply = Mockito.mock(ListVariableStateSupply.class);
        Mockito.when(scoreDirector.getListVariableStateSupply(Mockito.any())).thenReturn(listStateSupply);

        // The list variable listeners are not running, so the element shadow variables are set by hand.
        link(listStateSupply, vehicleA, a1, null, a2, 0);
        link(listStateSupply, vehicleA, a2, a1, null, 1);
        link(listStateSupply, vehicleB, b1, null, b2, 0);
        link(listStateSupply, vehicleB, b2, b1, null, 1);
        link(listStateSupply, null, a3, null, null, -1);

        var graph = DefaultShadowVariableSessionFactory.buildListElementCascadeGraph(
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(
                        solutionDescriptor, ChangedVariableNotifier.of(scoreDirector),
                        b2, vehicleB, a1, a3, vehicleA, b1, a2),
                graphStructureAndDirection);

        // Construction bootstraps from null values: vehicle A's chain is walked once,
        // but vehicle B's chain is walked once before A's endTime has propagated and
        // once after it — one extra pass per vehicle chain level, a one-off cost.
        // The incremental update below pins the steady-state cost.
        assertThat(List.of(a1, a2, a3)).allMatch(visit -> visit.getCalledCount() == 1);
        assertThat(List.of(b1, b2)).allMatch(visit -> visit.getCalledCount() == 2);
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
        // Everything downstream is recomputed exactly once...
        assertThat(a3.getCalledCount()).isOne();
        assertThat(vehicleA.getEndTimeCalledCount()).isOne();
        assertThat(vehicleB.getPreviousEndTimeCalledCount()).isOne();
        assertThat(b1.getCalledCount()).isOne();
        assertThat(b2.getCalledCount()).isOne();
        // ...except vehicle B's endTime: the inner graph recomputes it through its
        // previousEndTime edge before the cascade re-walks B's chain, and once after.
        assertThat(vehicleB.getEndTimeCalledCount()).isEqualTo(2);
        assertThat(vehicleB.getEndTime()).isEqualTo(5);
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
