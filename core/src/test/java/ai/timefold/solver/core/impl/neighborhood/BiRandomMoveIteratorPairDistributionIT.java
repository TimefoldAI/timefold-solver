package ai.timefold.solver.core.impl.neighborhood;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.neighborhood.MoveProvider;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
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
 * End-to-end regressions around {@code BiRandomMoveIterator}'s per-pair fairness fix, using 5 entities with
 * skewed partner counts (10/20/40/80/160, 310 pairs total): (a) the fix itself, at a larger sample than
 * {@link BiRandomMoveIteratorPairProbabilityTest}; (b) a same-shape regression guard on
 * {@code CachedBiDatasetInstance}, unaffected by this fix since it draws from all materialized pairs directly,
 * with no per-left walk at all; (c) a completeness (not distribution) guard on the just-in-time backing's
 * whole-dataset unique drain; (d) confirmation that a filtering()-only shape, the fix's structural no-op case,
 * is untouched.
 */
@Execution(ExecutionMode.CONCURRENT)
class BiRandomMoveIteratorPairDistributionIT {

    private static final List<Integer> BUCKET_SIZE_LIST = List.of(10, 20, 40, 80, 160);
    private static final int TOTAL_PAIR_COUNT = 310; // Sum of BUCKET_SIZE_LIST.

    @Test
    void indexedJoin_skewedBucketsAreUniformPerPair() {
        var drawCount = 3_100_000;
        var fixture = new SkewedFixture();
        var context = NeighborhoodTester.build(new PickMatchingBucket(fixture.variable), TestdataSolution.buildMetaModel())
                .using(fixture.solution);
        var iterator = context.getMovesAsIterator(move -> move);

        var counts = new HashMap<Move<TestdataSolution>, Integer>();
        for (var i = 0; i < drawCount; i++) {
            counts.merge(iterator.next(), 1, Integer::sum);
        }

        assertThat(counts).hasSize(TOTAL_PAIR_COUNT);
        var expected = drawCount / (double) TOTAL_PAIR_COUNT;
        for (var entry : counts.entrySet()) {
            assertThat(entry.getValue())
                    .as("pair %s drawn %d times, expected close to %.0f (uniform over %d pairs)",
                            entry.getKey(), entry.getValue(), expected, TOTAL_PAIR_COUNT)
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    @Test
    void cachedJoin_sameSkewedShapeStaysUniformOverAllMaterializedPairs() {
        var drawCount = 620_000;
        var fixture = new SkewedFixture();
        var moveStreamFactory = new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(),
                EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var cachedDataset = moveStreamFactory.register(entityStream.join(valueStream, fixture.joiner()));

        var session = createSession(moveStreamFactory, fixture.solution);
        var instance = session.getInstance(cachedDataset);

        var random = new Random(0);
        var counts = new HashMap<List<Object>, Integer>();
        var iterator = instance.randomIterator(random);
        for (var i = 0; i < drawCount; i++) {
            iterator.next();
            counts.merge(Arrays.asList(iterator.getA(), iterator.getB()), 1, Integer::sum);
        }

        assertThat(counts).hasSize(TOTAL_PAIR_COUNT);
        var expected = drawCount / (double) TOTAL_PAIR_COUNT;
        for (var entry : counts.entrySet()) {
            assertThat(entry.getValue())
                    .as("pair %s drawn %d times, expected close to %.0f (uniform over %d materialized pairs)",
                            entry.getKey(), entry.getValue(), expected, TOTAL_PAIR_COUNT)
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    /**
     * {@code JustInTimeBiDatasetInstance}'s whole-dataset {@code uniqueRandomIterator()} (as opposed to its
     * per-A flavor) does not override {@code acceptLeft}, so it is unaffected by the fairness fix; a full drain
     * must still visit every pair exactly once. See the {@code ponytail:} comment on
     * {@code UniqueRandomBiIterator} for why only its first draw is exactly pair-uniform, unlike
     * {@code BiRandomMoveIterator}'s every draw.
     */
    @Test
    void justInTimeJoin_uniqueDrainCoversEveryPairExactlyOnce() {
        var fixture = new SkewedFixture();
        var moveStreamFactory = new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(),
                EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var entityDataset = moveStreamFactory.register(entityStream);
        var justInTimeDataset = entityDataset.join(valueStream, fixture.joiner());

        var session = createSession(moveStreamFactory, fixture.solution);
        var instance = session.getInstance(justInTimeDataset);

        var seenPairSet = new HashSet<List<Object>>();
        var iterator = instance.uniqueRandomIterator(new Random(0));
        while (iterator.hasNext()) {
            iterator.next();
            List<Object> pair = Arrays.asList(iterator.getA(), iterator.getB());
            assertThat(seenPairSet.add(pair)).as("pair %s must not be visited twice", pair).isTrue();
        }
        assertThat(seenPairSet).hasSize(TOTAL_PAIR_COUNT);
    }

    /**
     * A filtering()-only joiner has no composite key to restrict the right side by, so {@code weight == bound}
     * for every left always: the fairness fix is a structural no-op here, deliberately. This only checks
     * reachability and a loose starvation floor, not uniformity, since the old per-left bias is still present.
     */
    @Test
    void filteringOnlyJoin_biasIsUnchangedButEveryPairStaysReachable() {
        var drawCount = 620_000;
        var fixture = new SkewedFixture();
        var context = NeighborhoodTester
                .build(new PickMatchingBucketFiltering(fixture.variable), TestdataSolution.buildMetaModel())
                .using(fixture.solution);
        var iterator = context.getMovesAsIterator(move -> move);

        var smallestBucketMoves = fixture.movesFor(0);
        var largestBucketMoves = fixture.movesFor(BUCKET_SIZE_LIST.size() - 1);
        var smallestCount = 0;
        var largestCount = 0;
        for (var i = 0; i < drawCount; i++) {
            var move = iterator.next();
            if (smallestBucketMoves.contains(move)) {
                smallestCount++;
            } else if (largestBucketMoves.contains(move)) {
                largestCount++;
            }
        }

        assertThat(smallestCount).isPositive();
        assertThat(largestCount).isPositive();
        var smallestPerPairRate = smallestCount / (double) BUCKET_SIZE_LIST.get(0);
        var largestPerPairRate = largestCount / (double) BUCKET_SIZE_LIST.get(BUCKET_SIZE_LIST.size() - 1);
        // Old, still-present bias: a pair in the smallest bucket is drawn far more often, per pair.
        // The exact ratio is BUCKET_SIZE_LIST's largest/smallest (=16); a loose floor avoids flakiness.
        assertThat(smallestPerPairRate)
                .as("filtering()-only join must keep its old per-left bias (small: %s, large: %s)",
                        smallestPerPairRate, largestPerPairRate)
                .isGreaterThan(largestPerPairRate * 5);
    }

    private static DefaultNeighborhoodSession<TestdataSolution> createSession(
            DefaultMoveStreamFactory<TestdataSolution> moveStreamFactory, TestdataSolution solution) {
        var scoreDirector = new EasyScoreDirectorFactory<>(moveStreamFactory.getSolutionDescriptor(),
                s -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var session = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::insert);
        session.settle();
        return session;
    }

    /**
     * 5 entities ("b0".."b4"), each paired only with its own bucket's values ("b0-0", "b0-1", ...), with
     * skewed bucket sizes {@link #BUCKET_SIZE_LIST} (10/20/40/80/160, 310 values total).
     */
    private static final class SkewedFixture {

        private final PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable =
                TestdataSolution.buildMetaModel().genuineEntity(TestdataEntity.class).basicVariable("value",
                        TestdataValue.class);
        private final List<TestdataEntity> entityList = new ArrayList<>();
        private final List<TestdataValue> valueList = new ArrayList<>();
        private final TestdataSolution solution;

        SkewedFixture() {
            for (var bucketIndex = 0; bucketIndex < BUCKET_SIZE_LIST.size(); bucketIndex++) {
                entityList.add(new TestdataEntity("b" + bucketIndex));
                for (var i = 0; i < BUCKET_SIZE_LIST.get(bucketIndex); i++) {
                    valueList.add(new TestdataValue("b" + bucketIndex + "-" + i));
                }
            }
            solution = new TestdataSolution("solution");
            solution.setEntityList(entityList);
            solution.setValueList(valueList);
        }

        BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner() {
            return NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                    value -> value.getCode().split("-")[0]);
        }

        Set<Move<TestdataSolution>> movesFor(int bucketIndex) {
            var entity = entityList.get(bucketIndex);
            var moveSet = new HashSet<Move<TestdataSolution>>();
            for (var value : valueList) {
                if (value.getCode().startsWith(entity.getCode() + "-")) {
                    moveSet.add(Moves.change(variable, entity, value));
                }
            }
            return moveSet;
        }

    }

    @NullMarked
    private record PickMatchingBucket(PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            var matchingPrefix = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(
                    TestdataEntity::getCode, value -> value.getCode().split("-")[0]);
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, matchingPrefix)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

    @NullMarked
    private record PickMatchingBucketFiltering(
            PlanningVariableMetaModel<TestdataSolution, TestdataEntity, TestdataValue> variable)
            implements
                MoveProvider<TestdataSolution> {

        @Override
        public MoveStream<TestdataSolution> build(MoveStreamFactory<TestdataSolution> moveStreamFactory) {
            var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
            var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
            var matchingPrefix = NeighborhoodsJoiners.<TestdataSolution, TestdataEntity, TestdataValue> filtering(
                    (solutionView, entity, value) -> value.getCode().startsWith(entity.getCode() + "-"));
            return moveStreamFactory.pick(entityStream)
                    .pick(valueStream, matchingPrefix)
                    .asMove((solutionView, entity, value) -> Moves.change(variable, entity, value));
        }

    }

}
