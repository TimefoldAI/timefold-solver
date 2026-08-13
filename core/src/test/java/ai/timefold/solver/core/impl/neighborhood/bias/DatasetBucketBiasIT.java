package ai.timefold.solver.core.impl.neighborhood.bias;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DatasetBucketBiasIT extends AbstractBiasIT {

    private static final int TRIAL_COUNT = 200_000;

    @ValueSource(ints = { 1, 2, 5 })
    @ParameterizedTest
    void bucketsAreWeightedByRemainingSizeAtDraw(int drawIndex) {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner = NeighborhoodsJoiners.greaterThanOrEqual(
                entity -> Integer.parseInt(entity.getCode()), value -> Integer.parseInt(value.getCode()));
        // Just-in-time: the join is computed inside the BiDatasetInstance, via register(a).join(b).
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
        probe.setValue(valueList.getFirst());
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        solution.setValueList(valueList);

        var session = session(moveStreamFactory, solution);
        var instance = session.getInstance(justInTimeDataset);

        var root = new Random(0);
        tally("just-in-time per-A unique, draw #" + drawIndex, TRIAL_COUNT, trial -> {
            var splitRandom = splitFrom(root);
            var iterator = instance.uniqueRandomIterator(probe, splitRandom);
            TestdataValue pick = null;
            for (var i = 0; i < drawIndex; i++) {
                pick = iterator.next();
            }
            return Bucket.byKey(Integer.parseInt(Objects.requireNonNull(pick).getCode()));
        }).expectWeights(Bucket.weightMap()).assertWithinSigma(SIGMA_LIMIT);
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

        static Map<Bucket, Double> weightMap() {
            var weightByBucket = new EnumMap<Bucket, Double>(Bucket.class);
            for (var bucket : values()) {
                weightByBucket.put(bucket, bucket.weight);
            }
            return weightByBucket;
        }

    }

}
