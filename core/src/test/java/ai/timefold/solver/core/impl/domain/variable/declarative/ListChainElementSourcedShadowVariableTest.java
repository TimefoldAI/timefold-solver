package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.heuristic.move.SelectorBasedCompositeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SelectorBasedChangeMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListAssignMove;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.SelectorBasedListChangeMove;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataMultiEntityChainElementSourcedSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataMultiEntityChainElementSourcedVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataMultiEntityChainElementSourcedVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListChainVariableReferenceGraph} on a model whose post-chain variable is
 * sourced from the route alone, so nothing declares its dependency on the pre-chain variable the
 * route reads. The chain node's edges supply that order; without them the post-chain variable is
 * computed before the route has been walked, and a dependency loop running through the route is
 * invisible to the graph.
 * <p>
 * The model declares no inconsistency field, so a dependency loop makes the solution structurally
 * flawed rather than inconsistent.
 */
class ListChainElementSourcedShadowVariableTest {

    @Test
    void vehicleLoopThroughTheRouteRejectsTheMove() {
        var a1 = new TestdataMultiEntityChainElementSourcedVisit("a1", 2);
        var a2 = new TestdataMultiEntityChainElementSourcedVisit("a2", 3);
        var b1 = new TestdataMultiEntityChainElementSourcedVisit("b1", 4);
        var vehicleA = new TestdataMultiEntityChainElementSourcedVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainElementSourcedVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));

        var solution = new TestdataMultiEntityChainElementSourcedSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1));

        var solutionMetaModel = TestdataMultiEntityChainElementSourcedSolution.buildMetaModel();
        var previousVehicleMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainElementSourcedVehicle.class)
                .basicVariable("previousVehicle", TestdataMultiEntityChainElementSourcedVehicle.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);

        // Unchained: A starts at 0 -> [2, 5]; B starts at 10 -> [14].
        assertThat(a1.getEndServiceTime()).isEqualTo(2);
        assertThat(a2.getEndServiceTime()).isEqualTo(5);
        assertThat(vehicleA.getEndTime()).isEqualTo(5);
        assertThat(b1.getEndServiceTime()).isEqualTo(14);

        // Chaining A after B shifts A's whole route, which its endTime must reflect.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));
        assertThat(vehicleA.getStartTime()).isEqualTo(14);
        assertThat(a1.getEndServiceTime()).isEqualTo(16);
        assertThat(a2.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicleA.getEndTime()).isEqualTo(19);

        // Chaining B after A closes a loop that exists only through the two routes: B's startTime
        // feeds A's visits, which feed A's endTime, which feeds B's startTime.
        // The model has no inconsistency field, so the update gives up and the move is rejected.
        assertThatThrownBy(() -> context.execute(Moves.change(previousVehicleMetaModel, vehicleB, vehicleA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("structurally flawed");

        // Undoing the rejected move restores every value: the update that gave up kept the work of
        // the routes it did not get to, so this one walks them instead of leaving them stale.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleB, null));
        assertThat(b1.getEndServiceTime()).isEqualTo(14);
        assertThat(a1.getEndServiceTime()).isEqualTo(16);
        assertThat(a2.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicleA.getEndTime()).isEqualTo(19);
    }

    /**
     * A move that both dirties a route and closes a dependency loop. The update gives up on the
     * loop, possibly before it reached that route's chain node, so the route must stay dirty
     * for the update that follows the undo; dropping it would leave the route stale forever.
     */
    @Test
    void aMoveThatDirtiesARouteAndClosesALoopLeavesNothingStale() {
        var a1 = new TestdataMultiEntityChainElementSourcedVisit("a1", 2);
        var a2 = new TestdataMultiEntityChainElementSourcedVisit("a2", 3);
        var b1 = new TestdataMultiEntityChainElementSourcedVisit("b1", 4);
        var spare = new TestdataMultiEntityChainElementSourcedVisit("spare", 5);
        var vehicleA = new TestdataMultiEntityChainElementSourcedVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainElementSourcedVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));

        var solution = new TestdataMultiEntityChainElementSourcedSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1, spare));

        var solutionMetaModel = TestdataMultiEntityChainElementSourcedSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainElementSourcedVehicle.class);
        var previousVehicleMetaModel =
                vehicleMetaModel.basicVariable("previousVehicle", TestdataMultiEntityChainElementSourcedVehicle.class);
        var listVariableMetaModel = vehicleMetaModel.listVariable("visits", TestdataMultiEntityChainElementSourcedVisit.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);

        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));

        // Appending to B's route and chaining B after A at once: the routes are dirty and looped.
        assertThatThrownBy(() -> context.execute(Moves.compose(
                Moves.assign(listVariableMetaModel, spare, vehicleB, 1),
                Moves.change(previousVehicleMetaModel, vehicleB, vehicleA))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("structurally flawed");

        // Undoing the loop leaves B's route longer, which every value must reflect.
        context.execute(Moves.change(previousVehicleMetaModel, vehicleB, null));
        assertThat(b1.getEndServiceTime()).isEqualTo(14);
        assertThat(spare.getEndServiceTime()).isEqualTo(19);
        assertThat(vehicleB.getEndTime()).isEqualTo(19);
        assertThat(vehicleA.getStartTime()).isEqualTo(19);
        assertThat(a1.getEndServiceTime()).isEqualTo(21);
        assertThat(a2.getEndServiceTime()).isEqualTo(24);
        assertThat(vehicleA.getEndTime()).isEqualTo(24);
    }

    /**
     * A failed update keeps the chain nodes it did not process marked in the graph. A legacy composite
     * move changes the same route again in a single update, without an undo in between,
     * which must still walk what the failed update recorded.
     */
    @Test
    void anUpdateAfterAFailedOneWalksWhatTheFailedOneRecorded() {
        var a1 = new TestdataMultiEntityChainElementSourcedVisit("a1", 2);
        var b1 = new TestdataMultiEntityChainElementSourcedVisit("b1", 4);
        var b2 = new TestdataMultiEntityChainElementSourcedVisit("b2", 3);
        var b3 = new TestdataMultiEntityChainElementSourcedVisit("b3", 2);
        var b4 = new TestdataMultiEntityChainElementSourcedVisit("b4", 1);
        var spare = new TestdataMultiEntityChainElementSourcedVisit("spare", 5);
        var vehicleA = new TestdataMultiEntityChainElementSourcedVehicle("A", 0);
        var vehicleB = new TestdataMultiEntityChainElementSourcedVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1, b2, b3, b4)));

        var solution = new TestdataMultiEntityChainElementSourcedSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, b1, b2, b3, b4, spare));

        var solutionMetaModel = TestdataMultiEntityChainElementSourcedSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataMultiEntityChainElementSourcedVehicle.class);
        var previousVehicleMetaModel =
                vehicleMetaModel.basicVariable("previousVehicle", TestdataMultiEntityChainElementSourcedVehicle.class);
        var previousVehicleDescriptor =
                ((DefaultPlanningVariableMetaModel<TestdataMultiEntityChainElementSourcedSolution, TestdataMultiEntityChainElementSourcedVehicle, TestdataMultiEntityChainElementSourcedVehicle>) previousVehicleMetaModel)
                        .variableDescriptor();
        var listVariableDescriptor =
                ((DefaultPlanningListVariableMetaModel<TestdataMultiEntityChainElementSourcedSolution, TestdataMultiEntityChainElementSourcedVehicle, TestdataMultiEntityChainElementSourcedVisit>) vehicleMetaModel
                        .listVariable("visits", TestdataMultiEntityChainElementSourcedVisit.class))
                        .variableDescriptor();
        var context = MoveTester.build(solutionMetaModel).using(solution);
        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));

        // Inserting into B's route and chaining B after A, in one update: the loop makes it give up.
        assertThatThrownBy(() -> context.execute(SelectorBasedCompositeMove.buildMove(
                new SelectorBasedListAssignMove<>(listVariableDescriptor, spare, vehicleB, 1),
                new SelectorBasedChangeMove<>(previousVehicleDescriptor, vehicleB, vehicleA))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("structurally flawed");

        // Breaking the loop and moving b4 before b3, in one update without an undo in between.
        context.execute(SelectorBasedCompositeMove.buildMove(
                new SelectorBasedChangeMove<>(previousVehicleDescriptor, vehicleB, null),
                new SelectorBasedListChangeMove<>(listVariableDescriptor, vehicleB, 4, vehicleB, 3)));
        assertShadowsAreAtFixedPoint(solution);
    }

    private static void assertShadowsAreAtFixedPoint(TestdataMultiEntityChainElementSourcedSolution solution) {
        DeclarativeShadowVariableAssertions.assertShadowsAreAtFixedPoint(solution,
                s -> s.getVehicles().stream().map(TestdataMultiEntityChainElementSourcedVehicle::getStartTime).toList(),
                s -> s.getVehicles().stream().map(TestdataMultiEntityChainElementSourcedVehicle::getEndTime).toList(),
                s -> s.getVisits().stream().map(TestdataMultiEntityChainElementSourcedVisit::getEndServiceTime).toList());
    }
}
