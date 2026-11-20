package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Asserts that a change only recomputes the entities it can reach,
 * which is the point of {@link ListChainVariableReferenceGraph}.
 */
class ListChainVariableReferenceGraphTest {

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
        assertThat(graphStructureAndDirection.parentMetaModel().entity().type()).isEqualTo(TestdataMultiEntityChainVisit.class);

        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        var listVariableState = Mockito.mock(ListVariableState.class);
        Mockito.when(scoreDirector.getListVariableState(Mockito.any())).thenReturn(listVariableState);

        // The list variable listeners are not running, so the element shadow variables are set by hand.
        link(listVariableState, vehicleA, a1, null, 0);
        link(listVariableState, vehicleA, a2, a1, 1);
        link(listVariableState, vehicleB, b1, null, 0);
        link(listVariableState, vehicleB, b2, b1, 1);
        link(listVariableState, null, a3, null, -1);

        var graph = DefaultShadowVariableSessionFactory.buildListChainGraph(
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(
                        solutionDescriptor, ChangedVariableNotifier.of(scoreDirector),
                        b2, vehicleB, a1, a3, vehicleA, b1, a2));
        assertThat(graph).isInstanceOf(ListChainVariableReferenceGraph.class);
        graph.updateChanged();

        // The topological order puts vehicle A's chain node before vehicle B's,
        // so the first update computes every element exactly once.
        assertThat(List.of(a1, a2, b1, b2)).allSatisfy(visit -> assertThat(visit.getCalledCount()).isOne());
        assertThat(a3.getCalledCount()).isOne();
        assertThat(vehicleB.getEndTime()).isEqualTo(4);

        vehicleA.reset();
        vehicleB.reset();
        List.of(a1, a2, a3, b1, b2).forEach(TestdataMultiEntityChainVisit::reset);

        // Append a3 to the end of vehicle A's route, with the events the score director sends.
        var visitsMetaModel = solutionDescriptor.getMetaModel().entity(TestdataMultiEntityChainVehicle.class)
                .variable("visits");
        var visitList = solutionDescriptor.getListVariableDescriptor().getValue(vehicleA);
        var visitsHook = Objects.requireNonNull(graph.resolveHookFor(visitsMetaModel));
        visitsHook.beforeListVariableChanged(vehicleA, visitList, 2, 2);
        vehicleA.getVisits().add(a3);
        var visitMetaModel = solutionDescriptor.getMetaModel().entity(TestdataMultiEntityChainVisit.class);
        var vehicleHook = Objects.requireNonNull(graph.resolveHookFor(visitMetaModel.variable("vehicle")));
        var previousVisitHook = Objects.requireNonNull(graph.resolveHookFor(visitMetaModel.variable("previousVisit")));
        vehicleHook.beforeVariableChanged(a3);
        previousVisitHook.beforeVariableChanged(a3);
        link(listVariableState, vehicleA, a3, a2, 2);
        vehicleHook.afterVariableChanged(a3);
        previousVisitHook.afterVariableChanged(a3);
        visitsHook.afterListVariableChanged(vehicleA, visitList, 2, 3);
        graph.updateChanged();

        // The elements before the insertion point are unreachable from it and are left alone.
        assertThat(a1.getCalledCount()).isZero();
        assertThat(a2.getCalledCount()).isZero();
        // Pre-chain variables do not depend on the chain, so a chain-only change never recomputes them.
        assertThat(vehicleA.getPreviousEndTimeCalledCount()).isZero();
        // Everything downstream is recomputed exactly once:
        // the single pass in topological order visits vehicle A's chain node,
        // its endTime, vehicle B's previousEndTime, vehicle B's chain node
        // and finally vehicle B's endTime.
        assertThat(a3.getCalledCount()).isOne();
        assertThat(vehicleA.getEndTimeCalledCount()).isOne();
        assertThat(vehicleB.getPreviousEndTimeCalledCount()).isOne();
        assertThat(b1.getCalledCount()).isOne();
        assertThat(b2.getCalledCount()).isOne();
        assertThat(vehicleB.getEndTimeCalledCount()).isOne();
        assertThat(vehicleB.getEndTime()).isEqualTo(5);
    }

    private static void link(
            ListVariableState<TestdataMultiEntityChainSolution, TestdataMultiEntityChainVehicle, TestdataMultiEntityChainVisit> listVariableState,
            TestdataMultiEntityChainVehicle vehicle, TestdataMultiEntityChainVisit visit,
            TestdataMultiEntityChainVisit previousVisit, int index) {
        visit.setVehicle(vehicle);
        visit.setPreviousVisit(previousVisit);
        var position = vehicle == null ? ElementPosition.unassigned() : ElementPosition.of(vehicle, index);
        Mockito.when(listVariableState.getElementPosition(visit)).thenReturn(position);
    }
}
