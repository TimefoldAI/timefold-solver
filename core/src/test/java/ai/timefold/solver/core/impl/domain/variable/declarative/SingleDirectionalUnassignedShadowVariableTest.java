package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;
import ai.timefold.solver.core.testdomain.shadow.single_directional_unassign.TestdataSingleDirectionalUnassignEntity;
import ai.timefold.solver.core.testdomain.shadow.single_directional_unassign.TestdataSingleDirectionalUnassignSolution;
import ai.timefold.solver.core.testdomain.shadow.single_directional_unassign.TestdataSingleDirectionalUnassignValue;

import org.junit.jupiter.api.Test;

/**
 * Mimics, with custom moves, what a ruin and recreate move does when it ruins more than one visit:
 * {@code SelectorBasedListRuinRecreateMove} removes its whole ruined batch under a single
 * before/after bracket and calls {@code updateShadowVariables()} once, before its nested
 * construction heuristic reinserts any of them in later, separate passes - so the ruin pass always
 * has 2+ elements unassigned at once whenever it ruins 2 or more, regardless of how many of them a
 * later pass goes on to recreate.
 * <p>
 * A single-element {@code Moves.unassign(variableMetaModel, PositionInList)} does not reproduce
 * this: {@code MoveDirector} triggers its own {@code updateShadowVariables()} pass after every
 * individual primitive call, so composing two of them still runs two separate passes, each with
 * only one element unassigned - never two at once. Only a primitive that unassigns several elements
 * under one before/after bracket, such as the range-based {@code Moves.unassign(variableMetaModel,
 * Range)} used below, reproduces the batching a real ruin does.
 */
class SingleDirectionalUnassignedShadowVariableTest {

    @Test
    void ruiningTwoVisitsThenRecreatingOnlyOneUpdatesBoth() {
        var v1 = new TestdataSingleDirectionalUnassignValue("v1", 10);
        var v2 = new TestdataSingleDirectionalUnassignValue("v2", 20);
        var v3 = new TestdataSingleDirectionalUnassignValue("v3", 5);
        var v4 = new TestdataSingleDirectionalUnassignValue("v4", 1);

        var vehicle1 = new TestdataSingleDirectionalUnassignEntity("vehicle1");
        vehicle1.setValues(new ArrayList<>(List.of(v1, v2, v3)));
        var vehicle2 = new TestdataSingleDirectionalUnassignEntity("vehicle2");
        vehicle2.setValues(new ArrayList<>(List.of(v4)));

        var solution = new TestdataSingleDirectionalUnassignSolution("solution", List.of(vehicle1, vehicle2),
                List.of(v1, v2, v3, v4));

        var solutionMetaModel = TestdataSingleDirectionalUnassignSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataSingleDirectionalUnassignEntity.class)
                .listVariable("values", TestdataSingleDirectionalUnassignValue.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);

        // vehicle1: v1=[0,10), v2=[10,30), v3=[30,35). vehicle2: v4=[0,1).
        assertThat(v1.getArrivalTime()).isZero();
        assertThat(v2.getArrivalTime()).isEqualTo(10);
        assertThat(v3.getArrivalTime()).isEqualTo(30);
        assertThat(v4.getArrivalTime()).isZero();

        // The ruin: one move unassigns both v2 and v3 off vehicle1 under a single before/after
        // bracket, as SelectorBasedListRuinRecreateMove does for its whole ruined batch, before it
        // recreates any of it. Both are, for this one shadow variable pass, in no vehicle's route.
        context.execute(Moves.unassign(listVariableMetaModel, new Range<>(vehicle1, 1, 3)));

        // Neither v2 nor v3 is in any route, so neither has an arrival time: both stale values -
        // v2's 10 and v3's 30 - must be cleared, not just the one that happens to dequeue first.
        assertThat(v2.getEntity()).isNull();
        assertThat(v2.getArrivalTime()).isNull();
        assertThat(v3.getEntity()).isNull();
        assertThat(v3.getArrivalTime()).isNull();
        assertThat(v1.getArrivalTime()).isZero();

        // The recreate: a later, separate move reinserts only v2, onto vehicle2 after v4. v3 is
        // left unassigned, exactly as whatever a construction heuristic does not get around to
        // reinserting stays after a real ruin and recreate move.
        context.execute(Moves.assign(listVariableMetaModel, v2, vehicle2, 1));

        // v2 moved vehicles, so it must be recomputed fresh, not merely left non-null: after v4 it
        // is 1, not its old value of 10.
        assertThat(v2.getEntity()).isEqualTo(vehicle2);
        assertThat(v2.getArrivalTime()).isEqualTo(1);
        // v3 was never recreated and stays unassigned.
        assertThat(v3.getEntity()).isNull();
        assertThat(v3.getArrivalTime()).isNull();
        assertThat(v1.getArrivalTime()).isZero();
        assertThat(v4.getArrivalTime()).isZero();
    }

}
