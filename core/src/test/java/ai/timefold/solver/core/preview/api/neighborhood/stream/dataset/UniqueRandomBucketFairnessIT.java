package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * End-to-end regression for the multi-bucket unique-iterator fix ({@code MultiBucketUniqueRandomIterator}),
 * reached through the only route to it that is public API: a just-in-time bi-join's per-A
 * {@code uniqueRandomIterator}, backed by {@code ComparisonIndexer}. {@link NeighborhoodsJoiners} has no public
 * {@code containedIn}, so {@code ContainedInIndexer}'s flavor of the same fix cannot be reached from here at all;
 * {@link SelectionProbabilityTest} in the {@code impl} package covers that one directly.
 */
@Execution(ExecutionMode.CONCURRENT)
class UniqueRandomBucketFairnessIT {

    private static final int TRIAL_COUNT = 200_000;

    @Test
    void bucketsAreWeightedByRemainingSizeAtEveryDrawIndex() {
        for (var drawIndex : List.of(1, 2, 5)) {
            assertUniformAcrossBuckets(drawIndex);
        }
    }

    private void assertUniformAcrossBuckets(int drawIndex) {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner = NeighborhoodsJoiners.lessThanOrEqual(
                entity -> Integer.parseInt(entity.getCode()), value -> Integer.parseInt(value.getCode()));
        // Just-in-time: the join is computed inside the BiDatasetInstance, via register(a).join(b),
        // the same shape ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.DatasetTest uses.
        var entityDataset = moveStreamFactory.register(entityStream);
        var justInTimeDataset = entityDataset.join(valueStream, joiner);

        // One probing entity (key 50) and three value buckets (keys 10/20/30, sizes 2/3/5), all <= 50.
        var probe = new TestdataEntity("50");
        var valueList = new ArrayList<TestdataValue>();
        for (var bucket : Bucket.values()) {
            for (var i = 0; i < bucket.size; i++) {
                valueList.add(new TestdataValue(String.valueOf(bucket.key)));
            }
        }
        probe.setValue(valueList.get(0));
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        solution.setValueList(valueList);

        var scoreDirector = new EasyScoreDirectorFactory<>(moveStreamFactory.getSolutionDescriptor(),
                s -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var session = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::insert);
        session.settle();
        var instance = session.getInstance(justInTimeDataset);

        var random = new Random(0);
        var counts = new EnumMap<Bucket, Integer>(Bucket.class);
        for (var trial = 0; trial < TRIAL_COUNT; trial++) {
            var splitRandom = new Random(random.nextLong());
            var iterator = instance.uniqueRandomIterator(probe, splitRandom);
            TestdataValue pick = null;
            for (var i = 0; i < drawIndex; i++) {
                pick = iterator.next();
            }
            counts.merge(Bucket.byKey(Integer.parseInt(Objects.requireNonNull(pick).getCode())), 1, Integer::sum);
        }

        // Every bucket must be reachable at drawIndex; a leftover boundary-walk bug would starve all but one.
        assertThat(counts.keySet()).containsExactlyInAnyOrder(Bucket.values());

        for (var bucket : Bucket.values()) {
            var expected = TRIAL_COUNT * bucket.weight;
            var actual = counts.get(bucket);
            assertThat(actual)
                    .as(() -> "draw #%d: bucket %s picked %d times, expected close to %.0f (weight %.1f)."
                            .formatted(drawIndex, bucket, actual, expected, bucket.weight))
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    private enum Bucket {

        SMALL(10, 2, 0.2),
        MEDIUM(20, 3, 0.3),
        LARGE(30, 5, 0.5);

        private final int key;
        private final int size;
        private final double weight;

        Bucket(int key, int size, double weight) {
            this.key = key;
            this.size = size;
            this.weight = weight;
        }

        static Bucket byKey(int key) {
            for (var bucket : values()) {
                if (bucket.key == key) {
                    return bucket;
                }
            }
            throw new IllegalArgumentException("Unexpected key (%d).".formatted(key));
        }

    }

}
