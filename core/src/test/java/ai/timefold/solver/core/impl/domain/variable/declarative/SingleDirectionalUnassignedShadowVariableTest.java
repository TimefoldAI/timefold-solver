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
 * Any single move that unassigns 2 or more elements of the same list under one before/after
 * bracket triggers this bug, since that is what puts them in {@code changedEntities} together for
 * one {@code updateChanged()} pass. {@code Moves.unassign(variableMetaModel, Range)}, used below, is
 * not a custom move: it is {@code SubListUnassignMove}, the same move class
 * {@code SubListUnassignMoveProvider} draws from as an ordinary, standalone local search
 * neighborhood - no ruin and recreate needed - and that {@code SubListChangeMoveProvider} also
 * produces whenever its {@code crossingNull} targets an unassigned destination. This test only
 * skips drawing one at random and picks the span directly, to pin an exact, reproducible state.
 * <p>
 * A single-element {@code Moves.unassign(variableMetaModel, PositionInList)} does not reproduce
 * this: {@code MoveDirector} triggers its own {@code updateShadowVariables()} pass after every
 * individual primitive call, so composing two of them still runs two separate passes, each with
 * only one element unassigned - never two at once. Only a primitive that unassigns several elements
 * under one before/after bracket, such as the range-based move used below, reproduces the batching.
 * {@code SelectorBasedListRuinRecreateMove} is one other example of code that does this - it removes
 * its whole ruined batch under a single bracket too, before its nested construction heuristic
 * reinserts any of it - but it is not needed to observe the bug.
 */
class SingleDirectionalUnassignedShadowVariableTest {

    @Test
    void unassigningTwoVisitsAtOnceClearsBothArrivalTimes() {
        var v1 = new TestdataSingleDirectionalUnassignValue("v1", 10);
        var v2 = new TestdataSingleDirectionalUnassignValue("v2", 20);
        var v3 = new TestdataSingleDirectionalUnassignValue("v3", 5);

        var vehicle = new TestdataSingleDirectionalUnassignEntity("vehicle");
        vehicle.setValues(new ArrayList<>(List.of(v1, v2, v3)));

        var solution = new TestdataSingleDirectionalUnassignSolution("solution", List.of(vehicle),
                List.of(v1, v2, v3));

        var solutionMetaModel = TestdataSingleDirectionalUnassignSolution.buildMetaModel();
        var listVariableMetaModel = solutionMetaModel.genuineEntity(TestdataSingleDirectionalUnassignEntity.class)
                .listVariable("values", TestdataSingleDirectionalUnassignValue.class);

        var context = MoveTester.build(solutionMetaModel).using(solution);

        // vehicle: v1=[0,10), v2=[10,30), v3=[30,35).
        assertThat(v1.getArrivalTime()).isZero();
        assertThat(v2.getArrivalTime()).isEqualTo(10);
        assertThat(v3.getArrivalTime()).isEqualTo(30);

        // One SubListUnassignMove, nothing else: unassigns v2 and v3 together under a single
        // before/after bracket, so one updateShadowVariables() pass sees them both unassigned.
        context.execute(Moves.unassign(listVariableMetaModel, new Range<>(vehicle, 1, 3)));

        // Neither v2 nor v3 is in any route, so neither has an arrival time: both stale values -
        // v2's 10 and v3's 30 - must be cleared, not just the one that happens to dequeue first.
        assertThat(v2.getEntity()).isNull();
        assertThat(v2.getArrivalTime()).isNull();
        assertThat(v3.getEntity()).isNull();
        assertThat(v3.getArrivalTime()).isNull();
        // v1 was untouched by the move.
        assertThat(v1.getArrivalTime()).isZero();
    }

    @Test
    void reassigningOneOfTwoUnassignedVisitsRecomputesItFresh() {
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

        // Unassign v2 and v3 together, as in unassigningTwoVisitsAtOnceClearsBothArrivalTimes(),
        // then - in a later, separate move - reassign only v2, onto vehicle2 after v4. This is the
        // shape of a ruin and recreate move that cannot place all of its ruined batch: v3 is left
        // unassigned, exactly as whatever a construction heuristic does not get around to
        // reinserting stays after the real move.
        context.execute(Moves.unassign(listVariableMetaModel, new Range<>(vehicle1, 1, 3)));
        context.execute(Moves.assign(listVariableMetaModel, v2, vehicle2, 1));

        // v2 moved vehicles, so it must be recomputed fresh, not merely left non-null: after v4 it
        // is 1, not its old value of 10.
        assertThat(v2.getEntity()).isEqualTo(vehicle2);
        assertThat(v2.getArrivalTime()).isEqualTo(1);
        // v3 was never reassigned and stays unassigned.
        assertThat(v3.getEntity()).isNull();
        assertThat(v3.getArrivalTime()).isNull();
        assertThat(v1.getArrivalTime()).isZero();
        assertThat(v4.getArrivalTime()).isZero();
    }

}
