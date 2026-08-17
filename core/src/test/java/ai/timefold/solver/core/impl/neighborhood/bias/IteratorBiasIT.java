package ai.timefold.solver.core.impl.neighborhood.bias;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.bavet.common.index.CompositeKey;
import ai.timefold.solver.core.impl.bavet.common.index.Indexer;
import ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory;
import ai.timefold.solver.core.impl.bavet.common.index.RepeatingRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.index.RetiringRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.index.UniqueRandomIterator;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The bias fixes at the {@code bavet.common.index} iterator/indexer level:
 * uniformity of {@link RepeatingRandomIterator}, {@code DefaultRetiringRandomIterator} after retirement,
 * {@link UniqueRandomIterator}'s full-drain permutation order,
 * and both flavors' multi-bucket weighting by bucket size.
 */
class IteratorBiasIT extends AbstractBiasIT {

    private static final int SAMPLE_COUNT = 100;
    private static final List<Integer> SAMPLES = IntStream.range(0, SAMPLE_COUNT)
            .boxed()
            .toList();

    private static IntStream selectionCount() {
        var oneStream = IntStream.of(1);
        var multipleStream = IntStream.iterate(10, i -> i <= SAMPLE_COUNT, i -> i + 10);
        return IntStream.concat(oneStream, multipleStream);
    }

    @MethodSource("selectionCount") // Determines how many draws are made before recording the nth pick.
    @ParameterizedTest
    void repeatingRandomIteratorIsUniformAtDraw(int n) {
        var trialCount = 1_000_000;
        var sampleList = toEntries(SAMPLES);
        var root = new Random(0);
        tally("RepeatingRandomIterator uniform at draw #" + n, trialCount, trial -> {
            var splitRandom = splitFrom(root);
            var iterator = RepeatingRandomIterator.of(sampleList, splitRandom);
            Integer element = null;
            for (var i = 0; i < n; i++) {
                element = iterator.next();
            }
            return element;
        }).expectUniform(SAMPLES).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * Protects {@code ComparisonIndexer}'s plain (repeating, with-replacement) flavor:
     * it must pick across every matching bucket, weighted by bucket size,
     * not just the first bucket it encounters while walking the boundary.
     * Parameterized over both directions:
     * LTE walks {@code comparisonMap} forward,
     * GTE walks it in {@code reverseOrder},
     * and the two directions share no code path other than {@code collectMatchingBuckets} itself.
     * The GTE query (10) is also the smallest bucket's own key,
     * so it additionally decides the sign-flipped {@code hasOrEquals} branch under sampling.
     */
    @MethodSource("comparisonIndexerRandomIteratorWeightsByBucketSizeArguments")
    @ParameterizedTest
    void comparisonIndexerRandomIteratorWeightsByBucketSize(JoinerType joinerType, int queryAge) {
        var trialCount = 200_000;
        var joiner = (DefaultBiNeighborhoodsJoiner<TestPerson, TestPerson>) (joinerType == JoinerType.LESS_THAN_OR_EQUAL
                ? NeighborhoodsJoiners.lessThanOrEqual(TestPerson::age)
                : NeighborhoodsJoiners.greaterThanOrEqual(TestPerson::age));
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        // Three buckets, all matching the query, with sizes 1, 3, and 6 (weight 0.1 / 0.3 / 0.6).
        putBucket(indexer, 10, 1);
        putBucket(indexer, 20, 3);
        putBucket(indexer, 30, 6);

        var random = new Random(0);
        tally("ComparisonIndexer repeating, multi-bucket by size (%s)".formatted(joinerType), trialCount,
                trial -> Bucket.of(indexer.randomIterator(CompositeKey.of(queryAge), random).next()))
                .expectWeights(Bucket.weightMap())
                .assertWithinSigma(SIGMA_LIMIT);
    }

    private static List<Arguments> comparisonIndexerRandomIteratorWeightsByBucketSizeArguments() {
        return List.of(
                // Query 50: every bucket (10, 20, 30) is <= 50, so all three match, walked forward.
                Arguments.of(JoinerType.LESS_THAN_OR_EQUAL, 50),
                // Query 10: every bucket is >= 10, so all three match, walked in reverseOrder.
                Arguments.of(JoinerType.GREATER_THAN_OR_EQUAL, 10));
    }

    /**
     * A tuple reachable under more than one query key
     * must not be over-sampled relative to a tuple reachable under only one.
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
        tally("ContainingAnyOfIndexer unique, overlapping buckets", trialCount,
                trial -> indexer.uniqueRandomIterator(List.of("X", "Y"), random).next())
                .expectUniform(List.of(t1, t2, t3))
                .assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * Retiring an interior element used to oversample its surviving neighbors,
     * since a draw that landed on the retired index moved to whichever survivor was closest
     * instead of being redrawn from the live pool uniformly.
     */
    @Test
    void retiringRandomIteratorStaysUniformAfterRetirement() {
        var trialCount = 1_000_000;
        var elementCount = 20;
        var retiredElementSet = Set.of(5, 12); // Interior retirements; the old bug was unbiased at the edges.
        var elementList = IntStream.range(0, elementCount).boxed().toList();
        var survivorList = elementList.stream().filter(e -> !retiredElementSet.contains(e)).toList();

        var root = new Random(0);
        var report = tally("DefaultRetiringRandomIterator uniform after interior retirement", trialCount, trial -> {
            var splitRandom = splitFrom(root);
            var iterator = RetiringRandomIterator.of(toEntries(elementList), splitRandom);
            for (var retiredElement : retiredElementSet) {
                retireElement(iterator, retiredElement);
            }
            return iterator.next();
        });
        report.expectUniform(survivorList).assertWithinSigma(SIGMA_LIMIT);
    }

    /**
     * Draws (with replacement) until {@code target} is picked, then retires it.
     * Any non-matching draw along the way is left untouched,
     * exactly as an ordinary caller that decides not to retire what it just drew would leave it.
     */
    private static void retireElement(RetiringRandomIterator<Integer> iterator, int target) {
        while (!iterator.next().equals(target)) {
            // Keep drawing until the target comes up.
        }
        iterator.retire();
    }

    /**
     * Protects {@code DefaultRetiringRandomIterator}
     * (which {@code UniqueRandomIterator.of} builds on) against the same "snap to nearest active index" bias,
     * this time over a full drain:
     * every one of the {@code n!} possible draw orders must be equally likely.
     */
    @Test
    void uniqueRandomIteratorDrainsInUniformPermutationOrder() {
        var trialCount = 1_000_000;
        var elementList = List.of(0, 1, 2, 3, 4);

        var root = new Random(0);
        var report = tally("UniqueRandomIterator uniform full-drain permutation order", trialCount,
                trial -> {
                    var splitRandom = splitFrom(root);
                    var iterator = UniqueRandomIterator.of(toEntries(elementList), splitRandom);
                    return drainToList(iterator);
                });
        report.expectUniform(permutationsOf(elementList)).assertWithinSigma(SIGMA_LIMIT);
    }

    private static List<Integer> drainToList(UniqueRandomIterator<Integer> iterator) {
        List<Integer> drawOrder = new ArrayList<>();
        while (iterator.hasNext()) {
            drawOrder.add(iterator.next());
        }
        return drawOrder;
    }

    private static List<List<Integer>> permutationsOf(List<Integer> elementList) {
        if (elementList.isEmpty()) {
            return List.of(List.of());
        }
        var permutationList = new ArrayList<List<Integer>>();
        for (var i = 0; i < elementList.size(); i++) {
            var rest = new ArrayList<>(elementList);
            var picked = rest.remove(i);
            for (var suffix : permutationsOf(rest)) {
                var permutation = new ArrayList<Integer>(elementList.size());
                permutation.add(picked);
                permutation.addAll(suffix);
                permutationList.add(permutation);
            }
        }
        return permutationList;
    }

    /**
     * Protects {@code MultiBucketUniqueRandomIterator}
     * (built for a multi-key/multi-bucket unique query by both {@code ComparisonIndexer} and {@code ContainedInIndexer})
     * against the old bug where a boundary-ordered walk drained one bucket entirely before moving to the next,
     * so every draw was biased towards whichever bucket came first in that walk.
     * {@code drawIndex} 1 catches that first-draw bias directly;
     * 2 and 10 additionally catch a would-be continuation bug
     * (draining a bucket rather than re-sampling on every draw),
     * since in a correctly uniform without-replacement drain,
     * the bucket occupying any fixed draw position is weighted by bucket size exactly like the first,
     * by the same symmetry a uniformly shuffled deck has:
     * every position in the shuffle is equally likely to hold a card from any given suit,
     * in proportion to that suit's size.
     */
    @MethodSource("multiBucketUniqueRandomIteratorArguments")
    @ParameterizedTest
    void multiBucketUniqueRandomIteratorWeightsByBucketSize(MultiBucketFlavour flavour, int drawIndex) {
        var trialCount = 200_000;
        // Three buckets, sizes 1, 3, and 6 (weight 0.1 / 0.3 / 0.6).
        // Built once; a unique iterator's retire() never touches the underlying bucket,
        // so replaying draws against the same indexer across trials is safe.
        var indexer = flavour.buildIndexer();
        var root = new Random(0);
        tally("%s multi-bucket unique, draw #%d".formatted(flavour, drawIndex), trialCount, trial -> {
            var splitRandom = splitFrom(root);
            var iterator = flavour.uniqueRandomIterator(indexer, splitRandom);
            UniTuple<String> pick = null;
            for (var i = 0; i < drawIndex; i++) {
                pick = iterator.next();
            }
            return Bucket.of(pick);
        }).expectWeights(Bucket.weightMap()).assertWithinSigma(SIGMA_LIMIT);
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
     * The two indexers whose multi-bucket {@code uniqueRandomIterator} is backed by {@code MultiBucketUniqueRandomIterator}.
     * {@code ContainingAnyOfIndexer} is deliberately excluded:
     * its buckets can overlap, so no bucket weighting can make it uniform,
     * and it drains to a list instead.
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
     * Only used for their accessor method references,
     * to build a {@code ContainedInIndexer} directly via {@code JoinerType.CONTAINED_IN};
     * no instance of either is ever constructed,
     * since buckets here are populated directly by {@link #putContainedInBucket}.
     */
    private record TestWorker(List<String> skills) {
    }

    private record TestJob(String skill) {
    }

    private record TestPerson(int age) {
    }

    private record TestSkilledPerson(List<String> skills) {
    }

    private static void putContainedInBucket(Indexer<UniTuple<String>> indexer, int age, int size) {
        for (var i = 0; i < size; i++) {
            indexer.put(String.valueOf(age), UniTuple.of("age-" + age + "-" + i, 0));
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

        static Map<Bucket, Double> weightMap() {
            var weightByBucket = new EnumMap<Bucket, Double>(Bucket.class);
            for (var bucket : values()) {
                weightByBucket.put(bucket, bucket.weight);
            }
            return weightByBucket;
        }

    }

}
