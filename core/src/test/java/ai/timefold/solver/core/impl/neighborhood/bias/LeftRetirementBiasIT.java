package ai.timefold.solver.core.impl.neighborhood.bias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsPredicate;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

/**
 * End-to-end regressions for the two ways a left value's retirement can go wrong:
 * retiring it biases which surviving left is drawn next
 * ({@code DefaultRetiringRandomIterator}'s old "snap to nearest active index" correction),
 * or it gets retired even though it still has a real match
 * ({@code RetiringBiWalk}'s old single-probe bail-out).
 */
class LeftRetirementBiasIT extends AbstractBiasIT {

    /**
     * Before the fix, retiring an interior left entity biased its surviving neighbors;
     * only edge retirements were fair.
     * 4 interior entities (out of 20) are unreachable
     * ("dead": a bucket size of 0 gives them no matching value at all, per {@link BucketedFixture}),
     * which retires them for good during the warm-up phase;
     * the measured phase then checks that doing so left every survivor equally likely.
     */
    @Test
    void interiorRetirementsStayUniformOverSurvivingEntities() {
        var entityCount = 20;
        var deadIndexSet = Set.of(4, 8, 12, 16); // Interior; the old bug was already unbiased at the edges.
        var warmUpDrawCount = 1_000;
        var measuredDrawCount = 200_000;

        var bucketSizeList = IntStream.range(0, entityCount)
                .mapToObj(i -> deadIndexSet.contains(i) ? 0 : 1)
                .toList();
        var fixture = new BucketedFixture(bucketSizeList);
        var iterator = moveIterator(new PickPair(fixture.variable, fixture.joiner()), fixture.solution);

        // Give every dead entity plenty of chances to be drawn and permanently retired before measuring.
        for (var i = 0; i < warmUpDrawCount; i++) {
            iterator.next();
        }

        var liveEntityCodeList = new ArrayList<String>();
        for (var i = 0; i < entityCount; i++) {
            if (!deadIndexSet.contains(i)) {
                liveEntityCodeList.add(fixture.entityList.get(i).getCode());
            }
        }

        tally("interior retirement, live entities", measuredDrawCount, draw -> {
            var move = iterator.next();
            return ((TestdataEntity) move.getPlanningEntities().getFirst()).getCode();
        }).expectUniform(liveEntityCodeList).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * Under {@link NeighborhoodsJoiners#filtering(BiNeighborhoodsPredicate)},
     * {@code FilteringIterator}'s bail-out is a per-call false negative, not proof of emptiness:
     * before the fix, one such bail-out was enough to permanently retire a left that still had a real match,
     * and "hard" (1 match out of 20 values) permanently disappeared partway through this run.
     * After the fix, {@code RetiringBiWalk.PROBE_ATTEMPT_COUNT} independent probes are required before giving up.
     * "hard" must therefore still be drawn near the end of the run,
     * not only near the start.
     */
    @Test
    void hardToMatchEntityStaysReachableThroughoutTheRun() {
        var drawCount = 400_000;
        var tailDrawCount = 100_000;
        var variable = TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class)
                .basicVariable("value", TestdataValue.class);
        var hardEntity = new TestdataEntity("hard");
        var easyEntity = new TestdataEntity("easy");
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < 20; i++) {
            valueList.add(new TestdataValue("v" + i));
        }
        var hardMatch = valueList.getFirst();

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(hardEntity, easyEntity));
        solution.setValueList(valueList);

        var moveProvider = new PickHardOrEasy(variable, hardMatch);
        var iterator = moveIterator(moveProvider, solution);

        var hardCountInTail = new int[1];
        var report = tally("hard/easy filtering() reachability", drawCount, draw -> {
            var move = iterator.next();
            var isHard = move.getPlanningEntities().getFirst() == hardEntity;
            if (draw >= drawCount - tailDrawCount && isHard) {
                hardCountInTail[0]++;
            }
            return isHard ? "hard" : "easy";
        });

        // The core regression check: "hard" must still be reachable in the final stretch of the run,
        // not permanently retired somewhere in the middle after a single unlucky bail-out.
        assertThat(hardCountInTail[0])
                .as("'hard' must still be drawn in the final %d draws, not permanently retired", tailDrawCount)
                .isPositive();

        // Both entities have exactly one matching value's worth of moves,
        // so their overall share should be close to even (50/50),
        // not just individually positive.
        report.expectWeights(Map.of("hard", 0.5, "easy", 0.5)).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * Picks (entity, value) pairs matched by the given joiner.
     */
    @NullMarked
    private record PickPair(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable,
            BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner) implements MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, joiner)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

    /**
     * Picks (entity, value) pairs via a {@code filtering()} joiner
     * (no index, so the fairness fix from {@code PairFairnessBiasIT} is a structural no-op here):
     * "easy" matches every value, "hard" matches only {@code hardMatch}.
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
