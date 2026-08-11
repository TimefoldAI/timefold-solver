package ai.timefold.solver.core.impl.bavet.common.index;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.CONCURRENT)
final class SelectionProbabilityTest {

    private static final int TRIAL_COUNT = 1_000_000;
    private static final int SAMPLE_COUNT = 100;
    private static final List<Integer> SAMPLES = IntStream.range(0, SAMPLE_COUNT)
            .boxed()
            .toList();

    private static IntStream selectionCount() {
        var oneStream = IntStream.of(1);
        var multipleStream = IntStream.iterate(10, i -> i <= SAMPLE_COUNT, i -> i + 10);
        return IntStream.concat(oneStream, multipleStream);
    }

    @MethodSource("selectionCount") // Determines how many random picks are made.
    @ParameterizedTest
    void check(int n) {
        SortedMap<Integer, Integer> counts = new TreeMap<>();

        var sampleList = toEntries(SAMPLES);
        var random = new Random(0);
        for (var trial = 0; trial < TRIAL_COUNT; trial++) { // Independent trials; each gets its own random seed.
            Integer element = null;
            var splitRandom = new Random(random.nextLong());
            var iterator = RepeatingRandomIterator.of(sampleList, splitRandom); // This is the code that we test.
            for (var i = 0; i < n; i++) {
                element = iterator.next();
            }
            // Record the last picked element (nth element).
            counts.compute(element, (k, v) -> v == null ? 1 : v + 1);
        }

        // Guarantee that all numbers have been selected.
        Assertions.assertThat(counts).hasSize(SAMPLE_COUNT);

        // Guarantee that the distribution is roughly uniform.
        var statistics = new SummaryStatistics();
        for (var value : counts.values()) {
            statistics.addValue(value);
        }

        var context = MathContext.DECIMAL32;
        var standardDeviation = new BigDecimal(statistics.getStandardDeviation())
                .round(context);
        var threshold = BigDecimal.valueOf(TRIAL_COUNT / (double) SAMPLE_COUNT)
                .multiply(BigDecimal.valueOf(0.02))
                .round(context); // 2% tolerance
        Assertions.assertThat(standardDeviation)
                .as(() -> "Standard deviation of selection counts (%s) on the %sth random sample is over %s threshold."
                        .formatted(standardDeviation, n, threshold))
                .isLessThanOrEqualTo(threshold);
    }

    static <T> ElementAwareArrayList<T> toEntries(List<T> elements) {
        var list = new ElementAwareArrayList<T>();
        list.addAll(elements);
        return list;
    }

    /**
     * Protects solver fairness for {@link ComparisonIndexer}'s plain flavor:
     * it must pick across every matching bucket, weighted by bucket size,
     * not just the first bucket it encounters while walking the boundary,
     * which is all the exhausting {@link ComparisonIndexer.RandomIterator} needs to do,
     * since it eventually drains every bucket regardless of visit order.
     */
    @Test
    void comparisonIndexerRandomIteratorWeightsByBucketSize() {
        var trialCount = 200_000;
        var joiner = (DefaultBiNeighborhoodsJoiner<TestPerson, TestPerson>) NeighborhoodsJoiners
                .lessThanOrEqual(TestPerson::age);
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        // Three buckets, all matching a query of age 50, with sizes 1, 3, and 6 (weight 0.1 / 0.3 / 0.6).
        putBucket(indexer, 10, 1);
        putBucket(indexer, 20, 3);
        putBucket(indexer, 30, 6);

        var random = new Random(0);
        var counts = new EnumMap<Bucket, Integer>(Bucket.class);
        for (var trial = 0; trial < trialCount; trial++) {
            var iterator = indexer.randomIterator(CompositeKey.of(50), random);
            var pick = iterator.next();
            counts.merge(Bucket.of(pick), 1, Integer::sum);
        }

        // Every bucket must be reachable; a leftover boundary-walk bug would starve everything but the first.
        Assertions.assertThat(counts.keySet()).containsExactlyInAnyOrder(Bucket.values());

        for (var bucket : Bucket.values()) {
            var expected = trialCount * bucket.weight;
            var actual = counts.get(bucket);
            Assertions.assertThat(actual)
                    .as(() -> "Bucket %s picked %d times, expected close to %.0f (weight %.1f)."
                            .formatted(bucket, actual, expected, bucket.weight))
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    private static void putBucket(Indexer<UniTuple<String>> indexer, int age, int size) {
        for (var i = 0; i < size; i++) {
            indexer.put(CompositeKey.of(age), UniTuple.of("age-" + age + "-" + i, 0));
        }
    }

    private enum Bucket {

        SMALL(10, 0.1),
        MEDIUM(20, 0.3),
        LARGE(30, 0.6);

        private final int age;
        private final double weight;

        Bucket(int age, double weight) {
            this.age = age;
            this.weight = weight;
        }

        static Bucket of(UniTuple<String> tuple) {
            var fact = Objects.requireNonNull(tuple.getA());
            for (var bucket : values()) {
                if (fact.startsWith("age-" + bucket.age + "-")) {
                    return bucket;
                }
            }
            throw new IllegalArgumentException("Unexpected tuple (%s).".formatted(tuple));
        }

    }

}
