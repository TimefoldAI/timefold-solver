package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataElementSourcedSolution;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataElementSourcedVehicle;
import ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced.TestdataElementSourcedVisit;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link ListElementBlockVariableReferenceGraph} on a model whose post-chain variable is
 * sourced from the route alone, so nothing declares its dependency on the pre-chain variable the
 * route reads. The block node's edges supply that order; without them the post-chain variable is
 * computed before the route has been walked, and a dependency loop running through the route is
 * invisible to the graph.
 * <p>
 * The model declares no inconsistency field, so a dependency loop makes the solution structurally
 * flawed rather than inconsistent.
 */
class ListElementBlockElementSourcedShadowVariableTest {

    @Test
    void changeOnPredecessorVehiclePropagatesThroughTheRoute() {
        var a1 = new TestdataElementSourcedVisit("a1", 2);
        var a2 = new TestdataElementSourcedVisit("a2", 3);
        var b1 = new TestdataElementSourcedVisit("b1", 4);
        var vehicleA = new TestdataElementSourcedVehicle("A", 0);
        var vehicleB = new TestdataElementSourcedVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));

        var solution = new TestdataElementSourcedSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1));

        var solutionMetaModel = TestdataElementSourcedSolution.buildMetaModel();
        var previousVehicleMetaModel = solutionMetaModel.genuineEntity(TestdataElementSourcedVehicle.class)
                .basicVariable("previousVehicle", TestdataElementSourcedVehicle.class);
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
    }

    @Test
    void vehicleLoopThroughTheRouteRejectsTheMove() {
        var a1 = new TestdataElementSourcedVisit("a1", 2);
        var a2 = new TestdataElementSourcedVisit("a2", 3);
        var b1 = new TestdataElementSourcedVisit("b1", 4);
        var vehicleA = new TestdataElementSourcedVehicle("A", 0);
        var vehicleB = new TestdataElementSourcedVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));

        var solution = new TestdataElementSourcedSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1));

        var solutionMetaModel = TestdataElementSourcedSolution.buildMetaModel();
        var previousVehicleMetaModel = solutionMetaModel.genuineEntity(TestdataElementSourcedVehicle.class)
                .basicVariable("previousVehicle", TestdataElementSourcedVehicle.class);
        var context = MoveTester.build(solutionMetaModel).using(solution);

        context.execute(Moves.change(previousVehicleMetaModel, vehicleA, vehicleB));

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
     * loop, possibly before it reached that route's block node, so it must keep the route's dirty
     * range for the update that follows the undo; dropping it would leave the route stale forever.
     */
    @Test
    void aMoveThatDirtiesARouteAndClosesALoopLeavesNothingStale() {
        var a1 = new TestdataElementSourcedVisit("a1", 2);
        var a2 = new TestdataElementSourcedVisit("a2", 3);
        var b1 = new TestdataElementSourcedVisit("b1", 4);
        var spare = new TestdataElementSourcedVisit("spare", 5);
        var vehicleA = new TestdataElementSourcedVehicle("A", 0);
        var vehicleB = new TestdataElementSourcedVehicle("B", 10);
        vehicleA.setVisits(new ArrayList<>(List.of(a1, a2)));
        vehicleB.setVisits(new ArrayList<>(List.of(b1)));

        var solution = new TestdataElementSourcedSolution();
        solution.setVehicles(List.of(vehicleA, vehicleB));
        solution.setVisits(List.of(a1, a2, b1, spare));

        var solutionMetaModel = TestdataElementSourcedSolution.buildMetaModel();
        var vehicleMetaModel = solutionMetaModel.genuineEntity(TestdataElementSourcedVehicle.class);
        var previousVehicleMetaModel =
                vehicleMetaModel.basicVariable("previousVehicle", TestdataElementSourcedVehicle.class);
        var listVariableMetaModel = vehicleMetaModel.listVariable("visits", TestdataElementSourcedVisit.class);
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
}
