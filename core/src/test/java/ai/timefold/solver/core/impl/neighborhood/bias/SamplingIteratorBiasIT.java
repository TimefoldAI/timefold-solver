package ai.timefold.solver.core.impl.neighborhood.bias;

import static ai.timefold.solver.core.testutil.NeighborhoodTestUtils.createSession;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * End-to-end proof
 * that {@code samplingIterator} inherits the underlying {@code exhaustiveIterator}'s guaranteed post-retirement uniformity,
 * through the new sample-drawing surface rather than the raw iterator directly.
 * Covers both the Uni and Bi forms.
 */
class SamplingIteratorBiasIT extends AbstractBiasIT {

    /**
     * Each sample is assembled from a fresh {@code exhaustiveIterator} ({@code SampleAssembler.iterator}),
     * so drawing many size-1 samples is, per row, equivalent to
     * {@code IteratorBiasIT.repeatingRandomIteratorIsUniformAtDraw} at draw #1:
     * every row must be seeded about equally often.
     */
    @Test
    void uniSamplingIteratorSeedsEveryRowUniformly() {
        var trialCount = 200_000;
        var rowCount = 10;
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < rowCount; i++) {
            valueList.add(new TestdataValue("v" + i));
        }
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of());
        solution.setValueList(valueList);

        var dataset = moveStreamFactory.forEach(TestdataValue.class, false).asCachedDataset();
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(dataset);

        var root = new Random(0);
        BiasReport.tally("Uni samplingIterator, seed uniform over rows", trialCount, trial -> {
            var splitRandom = splitFrom(root);
            var sample = instance.samplingIterator(Samplers.exactly(1), splitRandom).next();
            return sample.iterator().next().getCode();
        }).expectUniform(valueList.stream().map(TestdataValue::getCode).toList()).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * One probing entity and three value buckets matched to it by an indexing {@code equal} joiner (sizes 2/3/5, weights
     * 0.2/0.3/0.5) —
     * mirrors {@code DatasetBucketBiasIT}'s fixture at the sample level instead of the raw dataset level.
     */
    @ValueSource(ints = { 1, 2, 5 })
    @ParameterizedTest
    void biSamplingIteratorMembersAreUniformAtEveryDrawPositionWithinASlice(int drawIndex) {
        var trialCount = 200_000;
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
        var justInTimeDataset = entityStream.asCachedDataset().join(valueStream, joiner);

        var probe = new TestdataEntity("probe");
        var valueList = new ArrayList<TestdataValue>();
        for (var bucket : Bucket.values()) {
            for (var i = 0; i < bucket.size; i++) {
                valueList.add(new TestdataValue("probe-" + bucket.name() + "-" + i));
            }
        }
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        solution.setValueList(valueList);

        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(justInTimeDataset);

        var root = new Random(0);
        BiasReport.tally("Bi samplingIterator, member uniform at draw #" + drawIndex, trialCount, trial -> {
            var splitRandom = splitFrom(root);
            var sample = instance.samplingIterator(probe, Samplers.exactly(drawIndex), splitRandom).next();
            var memberIterator = sample.iterator();
            TestdataValue member = null;
            for (var i = 0; i < drawIndex; i++) {
                member = memberIterator.next();
            }
            return Bucket.of(Objects.requireNonNull(member));
        }).expectWeights(Bucket.weightMap()).assertWithinSigma(SIGMA_LIMIT);
    }

    private enum Bucket {

        SMALL(2, 0.2),
        MEDIUM(3, 0.3),
        LARGE(5, 0.5);

        private final int size;
        private final double weight;

        Bucket(int size, double weight) {
            this.size = size;
            this.weight = weight;
        }

        static Bucket of(TestdataValue value) {
            for (var bucket : values()) {
                if (value.getCode().startsWith("probe-" + bucket.name() + "-")) {
                    return bucket;
                }
            }
            throw new IllegalArgumentException("Unexpected value (%s).".formatted(value));
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
