package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTester;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.assertj.core.data.Percentage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * End-to-end regression for {@code RetiringBiWalk}'s bounded-retry fix. Under a {@code filtering()} joiner,
 * {@link ai.timefold.solver.core.impl.neighborhood.stream.FilteringIterator}'s bail-out is a per-call false
 * negative, not proof of emptiness: before the fix, one such bail-out was enough to permanently retire a left
 * that still had a real match, and "hard" (1 match out of 20 values) permanently disappeared partway through
 * this run. After the fix, 3 independent probes are required before giving up, cutting the false-retirement
 * rate from ~{@code e^-10} to ~{@code e^-30}.
 */
@Execution(ExecutionMode.CONCURRENT)
class BiRandomMoveIteratorRetirementIT {

    private static final int DRAW_COUNT = 400_000;
    private static final int TAIL_DRAW_COUNT = 100_000;

    @Test
    void hardToMatchEntityStaysReachableThroughoutTheRun() {
        var variable = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);
        var hardEntity = new TestdataEntity("hard");
        var easyEntity = new TestdataEntity("easy");
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < 20; i++) {
            valueList.add(new TestdataValue("v" + i));
        }
        var hardMatch = valueList.get(0);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(hardEntity, easyEntity));
        solution.setValueList(valueList);

        var moveProvider = new PickHardOrEasy(variable, hardMatch);
        var context = NeighborhoodTester.build(moveProvider, TestdataSolution.buildMetaModel()).using(solution);
        var iterator = context.getMovesAsIterator(move -> move);

        var hardCount = 0;
        var easyCount = 0;
        var hardCountInTail = 0;
        for (var draw = 0; draw < DRAW_COUNT; draw++) {
            var move = iterator.next();
            var isHard = move.getPlanningEntities().getFirst() == hardEntity;
            if (isHard) {
                hardCount++;
            } else {
                easyCount++;
            }
            if (draw >= DRAW_COUNT - TAIL_DRAW_COUNT && isHard) {
                hardCountInTail++;
            }
        }

        // The core regression check: "hard" must still be reachable in the final stretch of the run,
        // not permanently retired somewhere in the middle after a single unlucky bail-out.
        assertThat(hardCountInTail)
                .as("'hard' must still be drawn in the final %d draws, not permanently retired", TAIL_DRAW_COUNT)
                .isPositive();

        // Both entities have exactly one matching value's worth of moves, so their overall share should
        // be close to even (50/50), not just individually positive.
        var hardShare = hardCount / (double) DRAW_COUNT;
        assertThat(hardShare)
                .as("'hard' share (%s) of all %d draws should be close to 50%%", hardShare, DRAW_COUNT)
                .isCloseTo(0.5, Percentage.withPercentage(5));
    }

    /**
     * Picks (entity, value) pairs via a {@code filtering()} joiner (no index, so the fairness fix from
     * {@code BiRandomMoveIteratorPairProbabilityTest} is a structural no-op here): "easy" matches every
     * value, "hard" matches only {@code hardMatch}.
     */
    @NullMarked
    private record PickHardOrEasy(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable,
            TestdataValue hardMatch) implements MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            var matchesUnlessHard = NeighborhoodsJoiners.<TestdataSolution, TestdataEntity, TestdataValue> filtering(
                    (solutionView, entity, value) -> !entity.getCode().equals("hard") || value.equals(hardMatch));
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, matchesUnlessHard)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
