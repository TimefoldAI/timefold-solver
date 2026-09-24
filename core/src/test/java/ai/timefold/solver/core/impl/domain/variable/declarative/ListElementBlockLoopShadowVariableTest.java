package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop.TestdataChainLoopSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop.TestdataChainLoopVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop.TestdataChainLoopVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListElementBlockVariableReferenceGraph} on a model whose vehicles chain to each
 * other through a planning variable, so the solver can put two of them in a dependency loop.
 * The elements read their vehicle's pre-chain start time, so a looped vehicle's whole route
 * is inconsistent with it and must not be computed.
 */
class ListElementBlockLoopShadowVariableTest {

    @Test
    void vehicleLoopMarksItsWholeRouteInconsistent() {
        var a1 = new TestdataChainLoopVisit("a1", 2);
        var a2 = new TestdataChainLoopVisit("a2", 3);
        var b1 = new TestdataChainLoopVisit("b1", 4);

        var vehicleA = new TestdataChainLoopVehicle("A", 0);
        var vehicleB = new TestdataChainLoopVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));

        var solution = new TestdataChainLoopSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1));

        var solutionMetaModel = TestdataChainLoopSolution.buildMetaModel();
        var previousVehicleMetaModel = solutionMetaModel.genuineEntity(TestdataChainLoopVehicle.class)
                .basicVariable("previousVehicle", TestdataChainLoopVehicle.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);
        // Unchained: A starts at 0 -> [2, 5]; B starts at 10 -> [14].
        assertThat(a1.getEndServiceTime()).isEqualTo(2);
        assertThat(a2.getEndServiceTime()).isEqualTo(5);
        assertThat(vehicleA.getEndTime()).isEqualTo(5);
        assertThat(b1.getEndServiceTime()).isEqualTo(14);

        // Chaining A after B is still a chain, so both routes stay consistent.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));
        assertThat(vehicleA.getStartTime()).isEqualTo(14);
        assertThat(a1.getEndServiceTime()).isEqualTo(16);
        assertThat(a2.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicleA.getInconsistent()).isFalse();

        // Chaining B after A closes the loop: A and B now depend on each other.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleB, vehicleA));
        assertThat(vehicleA.getInconsistent()).isTrue();
        assertThat(vehicleB.getInconsistent()).isTrue();
        // Their elements read a start time that has no defined value, all the way down the route.
        assertThat(a1.getInconsistent()).isTrue();
        assertThat(a2.getInconsistent()).isTrue();
        assertThat(b1.getInconsistent()).isTrue();
        assertThat(a1.getEndServiceTime()).isNull();
        assertThat(a2.getEndServiceTime()).isNull();
        assertThat(b1.getEndServiceTime()).isNull();

        // Breaking the loop brings both routes back.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleB, null));
        assertThat(vehicleA.getInconsistent()).isFalse();
        assertThat(vehicleB.getInconsistent()).isFalse();
        assertThat(a1.getInconsistent()).isFalse();
        assertThat(b1.getInconsistent()).isFalse();
        assertThat(b1.getEndServiceTime()).isEqualTo(14);
        assertThat(a1.getEndServiceTime()).isEqualTo(16);
        assertThat(a2.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicleA.getEndTime()).isEqualTo(19);
    }

    /**
     * A loop through routes reports their visits, as the arbitrary graph does, rather than the block nodes
     * standing for them: the visits are what the solver unassigns from an initial solution caught in a loop.
     */
    @Test
    void loopReportsTheVisitsOfTheRoutesItRunsThrough() {
        var a1 = new TestdataChainLoopVisit("a1", 2);
        var a2 = new TestdataChainLoopVisit("a2", 3);
        var b1 = new TestdataChainLoopVisit("b1", 4);
        var vehicleA = new TestdataChainLoopVehicle("A", 0);
        var vehicleB = new TestdataChainLoopVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));
        vehicleA.setPreviousVehicle(vehicleB);
        vehicleB.setPreviousVehicle(vehicleA);
        var solution = new TestdataChainLoopSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1));

        var solutionDescriptor = TestdataChainLoopSolution.buildSolutionDescriptor();
        var scoreDirector = new EasyScoreDirectorFactory<>(solutionDescriptor,
                (TestdataChainLoopSolution s) -> SimpleScore.of(0), EnvironmentMode.PHASE_ASSERT)
                .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var arbitraryGraph = DefaultShadowVariableSessionFactory.buildGraphForStructureAndDirection(
                new GraphStructure.GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null),
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(solutionDescriptor,
                        ChangedVariableNotifier.empty(), vehicleA, vehicleB, a1, a2, b1));
        assertThat(scoreDirector.computeVariableLoops()).isEqualTo(arbitraryGraph.getVariableLoops());
    }

    /**
     * The elements that leave a vehicle in a dependency loop become consistent again,
     * whether they move to a vehicle outside of it or are unassigned.
     */
    @Test
    void elementsLeavingALoopedVehicleBecomeConsistent() {
        var a1 = new TestdataChainLoopVisit("a1", 2);
        var a2 = new TestdataChainLoopVisit("a2", 3);
        var b1 = new TestdataChainLoopVisit("b1", 4);
        var c1 = new TestdataChainLoopVisit("c1", 1);

        var vehicleA = new TestdataChainLoopVehicle("A", 0);
        var vehicleB = new TestdataChainLoopVehicle("B", 10);
        var vehicleC = new TestdataChainLoopVehicle("C", 20);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));
        vehicleC.setVisits(new ArrayList<>(List.of(c1)));

        var solution = new TestdataChainLoopSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB, vehicleC));
        solution.setVisits(List.of(a1, a2, b1, c1));

        var solutionMetaModel = TestdataChainLoopSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataChainLoopVehicle.class);
        var previousVehicleMetaModel = vehicleMetaModel.basicVariable("previousVehicle", TestdataChainLoopVehicle.class);
        var listVariableMetaModel = vehicleMetaModel.listVariable("visits", TestdataChainLoopVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);

        // A after B, then B after A: the loop takes both routes down.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));
        context.execute(Moves.change(previousVehicleMetaModel, vehicleB, vehicleA));
        assertThat(a1.getInconsistent()).isTrue();
        assertThat(a2.getInconsistent()).isTrue();

        // a2 moves to the end of C's route, outside the loop; a1 stays in it.
        context.execute(Moves.change(listVariableMetaModel, vehicleA, 1, vehicleC, 1));
        assertThat(a2.getInconsistent()).isFalse();
        assertThat(a2.getEndServiceTime()).isEqualTo(24);
        assertThat(a1.getInconsistent()).isTrue();

        // Unassigned, a1 has no vehicle left to loop through.
        context.execute(Moves.unassign(listVariableMetaModel, vehicleA, 0));
        assertThat(a1.getInconsistent()).isFalse();
        assertThat(a1.getEndServiceTime()).isNull();
        assertThat(vehicleA.getInconsistent()).isTrue();
    }

    /**
     * A vehicle leaving a dependency loop gets back every element of its route, even when the same update
     * brings a consistent element to the head of that route, and the elements' recomputed values equal the
     * null the loop left them with, so that nothing seems to change along the way.
     */
    @Test
    void vehicleLeavingALoopBringsBackItsWholeRoute() {
        var a1 = new TestdataChainLoopVisit("a1", 2);
        var a2 = new TestdataChainLoopVisit("a2", 3);
        var b1 = new TestdataChainLoopVisit("b1", 4);
        var c1 = new TestdataChainLoopVisit("c1", 1);

        var vehicleA = new TestdataChainLoopVehicle("A", null); // Unknown departure, so no start time of its own.
        var vehicleB = new TestdataChainLoopVehicle("B", 10);
        var vehicleC = new TestdataChainLoopVehicle("C", 0);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));
        vehicleC.setVisits(new ArrayList<>(List.of(c1)));

        var solution = new TestdataChainLoopSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB, vehicleC));
        solution.setVisits(List.of(a1, a2, b1, c1));

        var solutionMetaModel = TestdataChainLoopSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataChainLoopVehicle.class);
        var previousVehicleMetaModel = vehicleMetaModel.basicVariable("previousVehicle", TestdataChainLoopVehicle.class);
        var previousVehicleDescriptor =
                ((DefaultPlanningVariableMetaModel<TestdataChainLoopSolution, TestdataChainLoopVehicle, TestdataChainLoopVehicle>) previousVehicleMetaModel)
                        .variableDescriptor();
        var listVariableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataChainLoopSolution, TestdataChainLoopVehicle, TestdataChainLoopVisit>) vehicleMetaModel
                        .listVariable("visits", TestdataChainLoopVisit.class))
                        .variableDescriptor();
        var context = MoveTester.build(solutionMetaModel).using(solution);

        // A after B, then B after A: the loop takes both routes down.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));
        context.execute(Moves.change(previousVehicleMetaModel, vehicleB, vehicleA));
        assertThat(a1.getInconsistent()).isTrue();
        assertThat(a2.getInconsistent()).isTrue();

        // In one update: A leaves the loop, back to its unknown start time, and C's visit moves to A's head.
        context.execute(SelectorBasedCompositeMove.buildMove(
                new SelectorBasedChangeMove<>(previousVehicleDescriptor, vehicleA, null),
                new SelectorBasedListChangeMove<>(listVariableDescriptor, vehicleC, 0, vehicleA, 0)));
        assertThat(vehicleA.getInconsistent()).isFalse();
        assertThat(c1.getInconsistent()).isFalse();
        assertThat(a1.getInconsistent()).isFalse();
        assertThat(a2.getInconsistent()).isFalse();
        assertThat(c1.getEndServiceTime()).isNull();
        assertThat(a1.getEndServiceTime()).isNull();
        assertThat(a2.getEndServiceTime()).isNull();
    }
}
