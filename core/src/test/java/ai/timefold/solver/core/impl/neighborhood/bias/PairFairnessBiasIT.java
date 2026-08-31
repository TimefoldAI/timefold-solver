package ai.timefold.solver.core.impl.neighborhood.bias;

import static ai.timefold.solver.core.testutil.NeighborhoodTestUtils.createSession;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * End-to-end regressions for {@code BiRandomMoveIterator}'s per-pair fairness fix:
 * a left picked uniformly and then rejected with probability {@code 1 - weight/bound}
 * makes the resulting pair probability uniform, not just the left draw,
 * so a pair in a small bucket must not be drawn any more (or less) often, per pair,
 * than a pair in a large bucket, for an indexing {@code equal} join.
 * A {@code filtering()}-only join has no composite key to restrict the right side by,
 * so {@code weight == bound} for every left always:
 * the fix is a structural no-op there, deliberately left alone.
 */
class PairFairnessBiasIT extends AbstractBiasIT {

    private static final List<Integer> BUCKET_SIZE_LIST = List.of(10, 20, 40, 80, 160);

    private static List<List<Integer>> bucketSizeLists() {
        // The small 2-bucket shape is weak enough that a filtering()-only join could still pass it,
        // which is why the larger, more skewed 5-bucket shape is also run.
        return List.of(List.of(2, 20), BUCKET_SIZE_LIST);
    }

    @MethodSource("bucketSizeLists")
    @ParameterizedTest
    void indexedJoin_bucketsAreUniformPerPair(List<Integer> bucketSizeList) {
        var drawCount = 620_000;
        var fixture = new BucketedFixture(bucketSizeList);
        var iterator = moveIterator(new PickPair(fixture.variable, fixture.joiner()), fixture.solution);

        var totalPairCount = bucketSizeList.stream().mapToInt(Integer::intValue).sum();
        var expectedShareByPair = new HashMap<Move<TestdataSolution>, Double>();
        for (var bucketIndex = 0; bucketIndex < bucketSizeList.size(); bucketIndex++) {
            for (var move : fixture.movesFor(bucketIndex)) {
                expectedShareByPair.put(move, 1.0 / totalPairCount);
            }
        }

        BiasReport.tally("indexed join, buckets %s".formatted(bucketSizeList), drawCount, draw -> iterator.next())
                .expectWeights(expectedShareByPair)
                .assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * A same-shape regression guard on {@code CachedBiDatasetInstance},
     * unaffected by the fix since it draws from all materialized pairs directly,
     * with no per-left walk at all.
     */
    @Test
    void cachedJoin_sameSkewedShapeStaysUniformOverAllMaterializedPairs() {
        var drawCount = 620_000;
        var fixture = new BucketedFixture(BUCKET_SIZE_LIST);
        var moveStreamFactory = new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(),
                EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var cachedDataset = entityStream.join(valueStream, fixture.joiner()).asCachedDataset();

        var session = createSession(moveStreamFactory, fixture.solution);
        var instance = session.getInstance(cachedDataset);
        var iterator = instance.iterator(new Random(0));

        var expectedPairList = new ArrayList<List<Object>>();
        for (var bucketIndex = 0; bucketIndex < BUCKET_SIZE_LIST.size(); bucketIndex++) {
            var entity = fixture.entityList.get(bucketIndex);
            for (var value : fixture.valueList) {
                if (value.getCode().startsWith(entity.getCode() + "-")) {
                    expectedPairList.add(List.of(entity, value));
                }
            }
        }

        BiasReport.tally("cached join, buckets %s".formatted(BUCKET_SIZE_LIST), drawCount, draw -> {
            iterator.next();
            return List.<Object> of(iterator.a(), iterator.b());
        }).expectUniform(expectedPairList).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * {@code JustInTimeBiDatasetInstance}'s whole-dataset {@code exhaustiveIterator()}
     * (as opposed to its per-A flavor)
     * does not override {@code acceptLeft},
     * so it is unaffected by the fairness fix;
     * a full drain must still visit every pair exactly once, regardless of bucket size.
     */
    @Test
    void justInTimeJoin_uniqueDrainCoversEveryPairExactlyOnce() {
        var totalPairCount = BUCKET_SIZE_LIST.stream().mapToInt(Integer::intValue).sum();
        var fixture = new BucketedFixture(BUCKET_SIZE_LIST);
        var moveStreamFactory = new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(),
                EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var entityDataset = entityStream.asCachedDataset();
        var justInTimeDataset = entityDataset.join(valueStream, fixture.joiner());

        var session = createSession(moveStreamFactory, fixture.solution);
        var instance = session.getInstance(justInTimeDataset);

        var seenPairSet = new HashSet<List<Object>>();
        var iterator = instance.exhaustiveIterator(new Random(0));
        while (iterator.hasNext()) {
            iterator.next();
            List<Object> pair = List.of(iterator.a(), iterator.b());
            assertThat(seenPairSet.add(pair)).as("pair %s must not be visited twice", pair).isTrue();
        }
        assertThat(seenPairSet).hasSize(totalPairCount);
    }

    /**
     * A filtering()-only joiner has no composite key to restrict the right side by,
     * so {@code weight == bound} for every left always:
     * the fairness fix is a structural no-op here, deliberately.
     * This only checks reachability and a loose starvation floor, not uniformity,
     * since the old per-left bias is still present and expected.
     */
    @Test
    void filteringOnlyJoin_biasIsUnchangedButEveryPairStaysReachable() {
        var drawCount = 620_000;
        var fixture = new BucketedFixture(BUCKET_SIZE_LIST);
        var iterator = moveIterator(new PickPair(fixture.variable, fixture.filteringJoiner()), fixture.solution);

        var smallestBucketMoves = fixture.movesFor(0);
        var largestBucketMoves = fixture.movesFor(BUCKET_SIZE_LIST.size() - 1);
        var smallestLabel = "smallest";
        var largestLabel = "largest";

        BiasReport.tally("filtering()-only join, buckets %s".formatted(BUCKET_SIZE_LIST), drawCount, draw -> {
            var move = iterator.next();
            if (smallestBucketMoves.contains(move)) {
                return smallestLabel;
            } else if (largestBucketMoves.contains(move)) {
                return largestLabel;
            }
            return "other";
        }).assertShareRatioAtLeast(smallestLabel, BUCKET_SIZE_LIST.getFirst(), largestLabel,
                BUCKET_SIZE_LIST.getLast(), 5);
    }

}
