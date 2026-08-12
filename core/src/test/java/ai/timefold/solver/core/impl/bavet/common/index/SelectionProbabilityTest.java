package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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
        assertThat(counts).hasSize(SAMPLE_COUNT);

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
        assertThat(standardDeviation)
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
     * Protects solver fairness for {@link ComparisonIndexer}'s plain (repeating, with-replacement) flavor:
     * it must pick across every matching bucket, weighted by bucket size,
     * not just the first bucket it encounters while walking the boundary.
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
        assertThat(counts.keySet()).containsExactlyInAnyOrder(Bucket.values());

        for (var bucket : Bucket.values()) {
            var expected = trialCount * bucket.weight;
            var actual = counts.get(bucket);
            assertThat(actual)
                    .as(() -> "Bucket %s picked %d times, expected close to %.0f (weight %.1f)."
                            .formatted(bucket, actual, expected, bucket.weight))
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    /**
     * A tuple reachable under more than one query key must not be over-sampled
     * relative to a tuple reachable under only one.
     */
    @Test
    void containingAnyOfUniqueIteratorIsUniformOverOverlappingBuckets() {
        var trialCount = 200_000;
        var joiner = new DefaultBiNeighborhoodsJoiner<>(TestSkilledPerson::skills, JoinerType.CONTAINING_ANY_OF,
                TestSkilledPerson::skills);
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        // Bucket X = {t1, t2}, bucket Y = {t2, t3}: t2 is reachable under both keys.
        var t1 = UniTuple.of("t1", 0);
        var t2 = UniTuple.of("t2", 0);
        var t3 = UniTuple.of("t3", 0);
        indexer.put(List.of("X"), t1);
        indexer.put(List.of("X", "Y"), t2);
        indexer.put(List.of("Y"), t3);

        var random = new Random(0);
        var counts = new HashMap<UniTuple<String>, Integer>();
        for (var trial = 0; trial < trialCount; trial++) {
            var iterator = indexer.uniqueRandomIterator(List.of("X", "Y"), random);
            var pick = iterator.next();
            counts.merge(pick, 1, Integer::sum);
        }

        assertThat(counts.keySet()).containsExactlyInAnyOrder(t1, t2, t3);

        var expected = trialCount / 3.0;
        for (var tuple : List.of(t1, t2, t3)) {
            var actual = counts.get(tuple);
            assertThat(actual)
                    .as(() -> "Tuple %s picked %d times, expected close to %.0f (uniform over 3 distinct tuples)."
                            .formatted(tuple, actual, expected))
                    .isCloseTo((int) expected, Percentage.withPercentage(5));
        }
    }

    /**
     * Protects {@link DefaultRetiringRandomIterator} against the old "snap to nearest active index"
     * correction: retiring an interior element used to oversample its surviving neighbors, since a draw
     * that landed on the retired index moved to whichever survivor was closest instead of being redrawn
     * from the live pool uniformly.
     */
    @Test
    void retiringRandomIteratorStaysUniformAfterRetirement() {
        var trialCount = 1_000_000;
        var elementCount = 20;
        var retiredElementSet = Set.of(5, 12); // Interior retirements; the old bug was unbiased at the edges.
        var elementList = IntStream.range(0, elementCount).boxed().toList();

        var random = new Random(0);
        var counts = new HashMap<Integer, Integer>();
        for (var trial = 0; trial < trialCount; trial++) { // Independent trials; each gets its own random seed.
            var splitRandom = new Random(random.nextLong());
            var iterator = new DefaultRetiringRandomIterator<>(toEntries(elementList), splitRandom);
            for (var retiredElement : retiredElementSet) {
                retireElement(iterator, retiredElement);
            }
            var pick = iterator.next();
            counts.merge(pick, 1, Integer::sum);
        }

        var survivorCount = elementCount - retiredElementSet.size();
        assertThat(counts.keySet()).hasSize(survivorCount);
        assertThat(counts.keySet()).noneMatch(retiredElementSet::contains);

        var expected = trialCount / (double) survivorCount;
        for (var entry : counts.entrySet()) {
            var actual = entry.getValue();
            assertThat(actual)
                    .as(() -> "Element %d picked %d times, expected close to %.0f (uniform over %d survivors)."
                            .formatted(entry.getKey(), actual, expected, survivorCount))
                    .isCloseTo((int) expected, Percentage.withPercentage(2));
        }
    }

    /**
     * Draws (with replacement) until {@code target} is picked, then retires it.
     * Any non-matching draw along the way is left untouched, exactly as an ordinary caller
     * that decides not to retire what it just drew would leave it.
     */
    private static void retireElement(RetiringRandomIterator<Integer> iterator, int target) {
        while (!iterator.next().equals(target)) {
            // Keep drawing until the target comes up.
        }
        iterator.retire();
    }

    /**
     * Protects {@link DefaultRetiringRandomIterator} (which {@link UniqueRandomIterator#of} builds on)
     * against the same "snap to nearest active index" bias, this time over a full drain:
     * every one of the {@code n!} possible draw orders must be equally likely.
     */
    @Test
    void uniqueRandomIteratorDrainsInUniformPermutationOrder() {
        var trialCount = 1_000_000;
        var elementList = List.of(0, 1, 2, 3, 4);
        var permutationCount = 120; // 5!

        var random = new Random(0);
        var counts = new HashMap<List<Integer>, Integer>();
        for (var trial = 0; trial < trialCount; trial++) { // Independent trials; each gets its own random seed.
            var splitRandom = new Random(random.nextLong());
            var iterator = UniqueRandomIterator.of(toEntries(elementList), splitRandom);
            var drawOrder = new ArrayList<Integer>(elementList.size());
            while (iterator.hasNext()) {
                drawOrder.add(iterator.next());
            }
            counts.merge(drawOrder, 1, Integer::sum);
        }

        assertThat(counts).hasSize(permutationCount);

        var expected = trialCount / (double) permutationCount;
        for (var entry : counts.entrySet()) {
            var actual = entry.getValue();
            assertThat(actual)
                    .as(() -> "Draw order %s occurred %d times, expected close to %.0f (uniform over %d permutations)."
                            .formatted(entry.getKey(), actual, expected, permutationCount))
                    .isCloseTo((int) expected, Percentage.withPercentage(5));
        }
    }

    /**
     * Protects {@link MultiBucketUniqueRandomIterator} (built for a multi-key/multi-bucket unique query by both
     * {@link ComparisonIndexer} and {@code ContainedInIndexer}) against the old bug where a boundary-ordered walk
     * drained one bucket entirely before moving to the next, so every draw was biased towards whichever bucket
     * came first in that walk. {@code drawIndex} 1 catches that first-draw bias directly; 2 and 10 additionally
     * catch a would-be continuation bug (draining a bucket rather than re-sampling on every draw), since in a
     * correctly uniform without-replacement drain, the bucket occupying ANY fixed draw position is weighted by
     * bucket size exactly like the first, by the same symmetry a uniformly shuffled deck has: every position in
     * the shuffle is equally likely to hold a card from any given suit, in proportion to that suit's size.
     */
    @MethodSource("multiBucketUniqueRandomIteratorArguments")
    @ParameterizedTest
    void multiBucketUniqueRandomIteratorWeightsByBucketSize(MultiBucketFlavour flavour, int drawIndex) {
        var trialCount = 200_000;
        // Three buckets, sizes 1, 3, and 6 (weight 0.1 / 0.3 / 0.6). Built once; a unique iterator's retire()
        // never touches the underlying bucket, so replaying draws against the same indexer across trials is safe.
        var indexer = flavour.buildIndexer();

        var random = new Random(0);
        var counts = new EnumMap<Bucket, Integer>(Bucket.class);
        for (var trial = 0; trial < trialCount; trial++) {
            var splitRandom = new Random(random.nextLong());
            var iterator = flavour.uniqueRandomIterator(indexer, splitRandom);
            UniTuple<String> pick = null;
            for (var i = 0; i < drawIndex; i++) {
                pick = iterator.next();
            }
            counts.merge(Bucket.of(pick), 1, Integer::sum);
        }

        // Every bucket must be reachable at drawIndex; a leftover boundary-walk bug would starve all but one.
        assertThat(counts.keySet()).containsExactlyInAnyOrder(Bucket.values());

        for (var bucket : Bucket.values()) {
            var expected = trialCount * bucket.weight;
            var actual = counts.get(bucket);
            assertThat(actual)
                    .as(() -> "%s draw #%d: bucket %s picked %d times, expected close to %.0f (weight %.1f)."
                            .formatted(flavour, drawIndex, bucket, actual, expected, bucket.weight))
                    .isCloseTo((int) expected, Percentage.withPercentage(10));
        }
    }

    private static List<Arguments> multiBucketUniqueRandomIteratorArguments() {
        var argumentsList = new ArrayList<Arguments>();
        for (var flavour : MultiBucketFlavour.values()) {
            for (var drawIndex : List.of(1, 2, 10)) {
                argumentsList.add(Arguments.of(flavour, drawIndex));
            }
        }
        return argumentsList;
    }

    /**
     * The two indexers whose multi-bucket {@code uniqueRandomIterator} is backed by
     * {@link MultiBucketUniqueRandomIterator}. {@code ContainingAnyOfIndexer} is deliberately excluded:
     * its buckets can overlap, so no bucket weighting can make it uniform (see the impossibility proof on
     * its own {@code uniqueRandomIteratorManyKeys} javadoc), and it drains to a list instead.
     */
    private enum MultiBucketFlavour {

        COMPARISON {

            @Override
            Indexer<UniTuple<String>> buildIndexer() {
                var joiner = (DefaultBiNeighborhoodsJoiner<TestPerson, TestPerson>) NeighborhoodsJoiners
                        .lessThanOrEqual(TestPerson::age);
                Indexer<UniTuple<String>> indexer = new IndexerFactory<>(joiner).buildIndexer(true);
                putBucket(indexer, 10, 1);
                putBucket(indexer, 20, 3);
                putBucket(indexer, 30, 6);
                return indexer;
            }

            @Override
            UniqueRandomIterator<UniTuple<String>> uniqueRandomIterator(Indexer<UniTuple<String>> indexer,
                    RandomGenerator random) {
                // Query age 50: every bucket (10, 20, 30) is <= 50, so all three match.
                return indexer.uniqueRandomIterator(CompositeKey.of(50), random);
            }

        },
        CONTAINED_IN {

            @Override
            Indexer<UniTuple<String>> buildIndexer() {
                var joiner = new DefaultBiNeighborhoodsJoiner<TestWorker, TestJob>(TestWorker::skills,
                        JoinerType.CONTAINED_IN, TestJob::skill);
                Indexer<UniTuple<String>> indexer = new IndexerFactory<>(joiner).buildIndexer(true);
                putContainedInBucket(indexer, 10, 1);
                putContainedInBucket(indexer, 20, 3);
                putContainedInBucket(indexer, 30, 6);
                return indexer;
            }

            @Override
            UniqueRandomIterator<UniTuple<String>> uniqueRandomIterator(Indexer<UniTuple<String>> indexer,
                    RandomGenerator random) {
                return indexer.uniqueRandomIterator(List.of("10", "20", "30"), random);
            }

        };

        abstract Indexer<UniTuple<String>> buildIndexer();

        abstract UniqueRandomIterator<UniTuple<String>> uniqueRandomIterator(Indexer<UniTuple<String>> indexer,
                RandomGenerator random);

    }

    /**
     * Only used for their accessor method references, to build a {@link ContainedInIndexer} directly via
     * {@code JoinerType.CONTAINED_IN}, the same way {@code ContainedInIndexerTest} does; no instance of either
     * is ever constructed, since buckets here are populated directly by {@link #putContainedInBucket}.
     */
    private record TestWorker(List<String> skills) {
    }

    private record TestJob(String skill) {
    }

    private static void putContainedInBucket(Indexer<UniTuple<String>> indexer, int age, int size) {
        for (var i = 0; i < size; i++) {
            indexer.put(String.valueOf(age), UniTuple.of("age-" + age + "-" + i, 0));
        }
    }

    private record TestSkilledPerson(List<String> skills) {
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
