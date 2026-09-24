package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.executeRandomListMove;
import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.solveWithFullAssert;
import static ai.timefold.solver.core.impl.domain.variable.declarative.DeclarativeShadowVariableAssertions.solveWithFullAssertAndEveryListMove;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainConstraintProvider;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain.TestdataMultiEntityChainVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataExtendedPriorityVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataExtendedSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataExtendedVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataExtendedVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataFactCycleSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataFactCycleVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback.TestdataFactCycleVisit;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_post_chain_reader.TestdataPostChainReaderSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_post_chain_reader.TestdataPostChainReaderVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_post_chain_reader.TestdataPostChainReaderVisit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests {@link ListElementBlockVariableReferenceGraph} on a model where
 * a vehicle starts where its predecessor vehicles end.
 */
class ListElementBlockShadowVariableTest {

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
    void swapWithinARoute() {
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

        // Swap the first and third visits.
        context.execute(Moves.swap(listVariableMetaModel, vehicle, 0, vehicle, 2));
        assertThat(v3.getEndServiceTime()).isEqualTo(3);
        assertThat(v2.getEndServiceTime()).isEqualTo(8);
        assertThat(v1.getEndServiceTime()).isEqualTo(9);
        assertThat(v4.getEndServiceTime()).isEqualTo(11);
        assertThat(vehicle.getEndTime()).isEqualTo(11);
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
     * An element that leaves a route is not recorded from the list change event, which the block
     * node only reads on the after event, but from the inverse and previous element changes the
     * list variable state supply notifies. Pins the argument
     * {@link ListElementBlockVariableReferenceGraph#beforeListVariableChanged} relies on.
     */
    @Test
    void removingTheLastElementOfARouteUpdatesItsEntity() {
        var x1 = new TestdataMultiEntityChainVisit("x1", 2);
        var x2 = new TestdataMultiEntityChainVisit("x2", 3);

        var vehicleA = new TestdataMultiEntityChainVehicle("A", 0);
        vehicleA.setVisits(new ArrayList<>(List.of(x1, x2)));

        var solution = new TestdataMultiEntityChainSolution();
        solution.setVehicles(List.of(vehicleA));
        solution.setVisits(List.of(x1, x2));

        var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                .listVariable("visits", TestdataMultiEntityChainVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(vehicleA.getEndTime()).isEqualTo(5);

        // The route's last element has no successor to notice its departure,
        // so only its own inverse and previous element changes record it.
        context.execute(Moves.unassign(listVariableMetaModel, vehicleA, 1));
        assertThat(x2.getEndServiceTime()).isNull();
        assertThat(x1.getEndServiceTime()).isEqualTo(2);
        assertThat(vehicleA.getEndTime()).isEqualTo(2);
        assertShadowsAreAtFixedPoint(solution);
    }

    /**
     * The block node edges overapproximate the per-element dependencies:
     * when the vehicles' fact dependencies form a cycle that is not a fixed loop
     * (endTime does not read startTime), the model falls back to the arbitrary graph,
     * where the loop only exists through the actual elements and the solver can break it.
     */
    @Test
    void cyclicVehicleFactsWithoutFixedLoopFallBack() {
        var vehicleA = new TestdataFactCycleVehicle(
                "A", 0);
        var vehicleB = new TestdataFactCycleVehicle(
                "B", 0);
        vehicleA.setPreviousVehicle(vehicleB);
        vehicleB.setPreviousVehicle(vehicleA);
        var v1 = new TestdataFactCycleVisit("v1", 1);
        var v2 = new TestdataFactCycleVisit("v2", 1);

        var solution =
                new TestdataFactCycleSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(v1, v2));

        var solutionMetaModel =
                TestdataFactCycleSolution
                        .buildMetaModel();
        var listVariableMetaModel = solutionMetaModel
                .genuineEntity(
                        TestdataFactCycleVehicle.class)
                .listVariable("visits",
                        TestdataFactCycleVisit.class);

        // With empty routes there is no dependency loop; both vehicles are consistent.
        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(vehicleA.isInconsistent()).isFalse();
        assertThat(vehicleB.isInconsistent()).isFalse();

        // Assigning visits to both vehicles creates the loop; the solver could break it later.
        context.execute(Moves.assign(listVariableMetaModel, v1, vehicleA, 0));
        context.execute(Moves.assign(listVariableMetaModel, v2, vehicleB, 0));
        assertThat(vehicleA.isInconsistent()).isTrue();
        assertThat(vehicleB.isInconsistent()).isTrue();

        // Unassigning vehicle A's visit breaks the loop again.
        context.execute(Moves.unassign(listVariableMetaModel, vehicleA, 0));
        assertThat(vehicleA.isInconsistent()).isFalse();
        assertThat(vehicleB.isInconsistent()).isFalse();
        assertThat(v2.getEndServiceTime()).isEqualTo(1);
        assertThat(vehicleB.getEndTime()).isEqualTo(1);
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

    /**
     * A visit reads the end time of its own vehicle, which the visits source:
     * the block node would have to be computed both before and after that end time,
     * so the build falls back to the arbitrary graph, whose per-visit nodes do not loop.
     */
    @Test
    void elementsReadingAPostChainVariableFallBack() {
        var v1 = new TestdataPostChainReaderVisit("v1", 2);
        var v2 = new TestdataPostChainReaderVisit("v2", 3);
        var v3 = new TestdataPostChainReaderVisit("v3", 4); // Initially unassigned.
        var vehicleA = new TestdataPostChainReaderVehicle("A");
        var vehicleB = new TestdataPostChainReaderVehicle("B");
        vehicleA.setVisits(new ArrayList<>(List.of(v1, v2)));
        var solution = new TestdataPostChainReaderSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(v1, v2, v3));

        var solutionDescriptor = TestdataPostChainReaderSolution.buildSolutionDescriptor();
        var entities = new Object[] { vehicleA, vehicleB, v1, v2, v3 };
        var graphStructureAndDirection = GraphStructure.determineGraphStructure(solutionDescriptor, entities);
        var scoreDirector = Mockito.mock(InnerScoreDirector.class);
        Mockito.when(scoreDirector.getListVariableState(Mockito.any())).thenReturn(Mockito.mock(ListVariableState.class));
        var graph = DefaultShadowVariableSessionFactory.buildGraphForStructureAndDirection(graphStructureAndDirection,
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(solutionDescriptor,
                        ChangedVariableNotifier.of(scoreDirector), entities));
        assertThat(graph).isNotInstanceOf(ListElementBlockVariableReferenceGraph.class);

        var solutionMetaModel = TestdataPostChainReaderSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataPostChainReaderVehicle.class)
                .listVariable("visits", TestdataPostChainReaderVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(vehicleA.getEndTime()).isEqualTo(5);
        assertThat(v1.getSlack()).isEqualTo(3);
        assertThat(v2.getSlack()).isZero();

        context.execute(Moves.assign(listVariableMetaModel, v3, vehicleA, 1));
        assertThat(vehicleA.getEndTime()).isEqualTo(9);
        assertThat(v1.getSlack()).isEqualTo(7);
        assertThat(v3.getSlack()).isEqualTo(3);
        context.execute(Moves.change(listVariableMetaModel, vehicleA, 0, vehicleB, 0));
        assertThat(vehicleB.getEndTime()).isEqualTo(2);
        assertThat(v1.getSlack()).isZero();
        DeclarativeShadowVariableAssertions.assertShadowsAreAtFixedPoint(solution,
                s -> s.getVehicles().stream().map(TestdataPostChainReaderVehicle::getEndTime).toList(),
                s -> s.getVisits().stream().map(TestdataPostChainReaderVisit::getEndServiceTime).toList(),
                s -> s.getVisits().stream().map(TestdataPostChainReaderVisit::getSlack).toList());
    }

    /**
     * Differential test: after every random move, the incrementally maintained shadow
     * variables must equal a from-scratch recomputation, which uses the arbitrary graph.
     */
    @Test
    void randomMovesStayAtFixedPoint() {
        for (var seed = 0; seed < 30; seed++) {
            var random = new Random(seed);
            var solution = generateSolution(true);
            var solutionMetaModel = TestdataMultiEntityChainSolution.buildMetaModel();
            var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainVehicle.class)
                    .listVariable("visits", TestdataMultiEntityChainVisit.class);
            var context = MoveTester.build(solutionMetaModel).using(solution);
            for (var moveIndex = 0; moveIndex < 40; moveIndex++) {
                executeRandomListMove(context, listVariableMetaModel, TestdataMultiEntityChainVehicle::getVisits,
                        solution.getVehicles(), solution.getVisits(), random);
                assertShadowsAreAtFixedPoint(solution);
            }
        }
    }

    @Test
    void solvingStaysAtFixedPoint() {
        assertShadowsAreAtFixedPoint(solve(generateSolution(false)));
    }

    @Test
    void solvingStaysAtFixedPointWithPreChainReadingElements() {
        assertShadowsAreAtFixedPoint(solve(generateSolution(true)));
    }

    @Test
    void solvingWithEveryListMoveStaysAtFixedPoint() {
        assertShadowsAreAtFixedPoint(solveWithFullAssertAndEveryListMove(TestdataMultiEntityChainSolution.class,
                TestdataMultiEntityChainConstraintProvider.class, generateSolution(true),
                TestdataMultiEntityChainVehicle.class, TestdataMultiEntityChainVisit.class));
    }

    /**
     * A visit subclass declares a declarative shadow variable the other visits of the same list
     * do not have, so the model falls back to the arbitrary graph, whose nodes are per entity
     * and variable; the block node's walk would apply the subclass's updater to every element.
     */
    @Test
    void declarativeVisitSubclassFallsBack() {
        var v1 = new TestdataExtendedVisit("v1", 1);
        var p2 = new TestdataExtendedPriorityVisit("p2", 2, 10);
        var v3 = new TestdataExtendedVisit("v3", 3);
        var vehicle = new TestdataExtendedVehicle("A", 0);
        vehicle.setVisits(new ArrayList<>(List.of(v1, p2, v3)));

        var solution = new TestdataExtendedSolution();
        solution.setVehicles(List.of(vehicle));
        solution.setVisits(List.of(v1, p2, v3));

        var solutionMetaModel = TestdataExtendedSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataExtendedVehicle.class)
                .listVariable("visits", TestdataExtendedVisit.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        assertThat(v1.getEndServiceTime()).isEqualTo(1);
        assertThat(p2.getEndServiceTime()).isEqualTo(3);
        assertThat(p2.getSlack()).isEqualTo(7);
        assertThat(v3.getEndServiceTime()).isEqualTo(6);
        assertThat(vehicle.getEndTime()).isEqualTo(6);

        // Swapping the priority visit to the head of the route shifts it and its own slack.
        context.execute(Moves.swap(listVariableMetaModel, vehicle, 0, vehicle, 1));
        assertThat(p2.getEndServiceTime()).isEqualTo(2);
        assertThat(p2.getSlack()).isEqualTo(8);
        assertThat(v1.getEndServiceTime()).isEqualTo(3);
        assertThat(v3.getEndServiceTime()).isEqualTo(6);
        assertThat(vehicle.getEndTime()).isEqualTo(6);
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
