package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

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
}
