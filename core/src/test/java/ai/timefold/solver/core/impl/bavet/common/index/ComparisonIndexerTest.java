package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ListEntry;

import org.junit.jupiter.api.Test;

/**
 * Covers the NON-unified (two parallel indexer) path:
 * joins whose indexing joiners contain no equal joiner,
 * so {@link IndexerFactory#isFusedEqualIndexEligible()} is false and the node keeps two comparison indexers.
 * Comparison is otherwise only exercised alongside a leading equal (see {@link EqualsAndComparisonIndexerTest});
 * a pure-comparison join takes the {@code useJoinIndex == false} branch and must still join correctly.
 * <p>
 * A single comparison joiner uses a {@link KeyUnpacker#single()},
 * so the index key is the raw (comparable) key, not a {@link CompositeKey};
 * the tests put/query with raw keys accordingly.
 */
class ComparisonIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<TestPerson, TestPerson> lessThanAge =
            (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.lessThan(TestPerson::age);

    @Test
    void joinIndexEligibilityRouting() {
        // No equal joiner ⇒ NOT eligible ⇒ the non-unified two-indexer path.
        assertThat(new IndexerFactory<>(lessThanAge).isFusedEqualIndexEligible()).isFalse();
        assertThat(new IndexerFactory<>(twoComparisons()).isFusedEqualIndexEligible()).isFalse();
        // A leading equal ⇒ eligible (boundary sanity check).
        assertThat(new IndexerFactory<>(equalGender()).isFusedEqualIndexEligible()).isTrue();
        assertThat(new IndexerFactory<>(equalThenLessThan()).isFusedEqualIndexEligible()).isTrue();
    }

    @Test
    void leftBridgeLessThan() {
        // Left bridge keeps LESS_THAN: a stored tuple matches query Q iff its key < Q.
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanAge).buildIndexer(true);
        var age30 = newTuple("age30");
        indexer.put(30, age30);
        var age40 = newTuple("age40");
        indexer.put(40, age40);
        var age40b = newTuple("age40b");
        indexer.put(40, age40b);

        assertThat(forEachToTuples(indexer, 50)).containsExactlyInAnyOrder(age30, age40, age40b);
        assertThat(forEachToTuples(indexer, 40)).containsExactlyInAnyOrder(age30);
        assertThat(forEachToTuples(indexer, 30)).isEmpty();
        assertThat(indexer.size(50)).isEqualTo(3);
        assertThat(indexer.size(40)).isEqualTo(1);
        assertThat(indexer.size(30)).isZero();
    }

    @Test
    void rightBridgeLessThanFlipsToGreaterThan() {
        // Right bridge flips LESS_THAN to GREATER_THAN: a stored tuple matches query Q iff its key > Q.
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanAge).buildIndexer(false);
        var age30 = newTuple("age30");
        indexer.put(30, age30);
        var age40 = newTuple("age40");
        indexer.put(40, age40);

        assertThat(forEachToTuples(indexer, 20)).containsExactlyInAnyOrder(age30, age40);
        assertThat(forEachToTuples(indexer, 30)).containsExactlyInAnyOrder(age40);
        assertThat(forEachToTuples(indexer, 40)).isEmpty();
        assertThat(indexer.size(20)).isEqualTo(2);
        assertThat(indexer.size(30)).isEqualTo(1);
    }

    @Test
    void putRemoveSize() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanAge).buildIndexer(true);
        assertThat(indexer.isRemovable()).isTrue();
        var age40 = newTuple("age40");
        var entry40 = indexer.put(40, age40);
        var age50 = newTuple("age50");
        var entry50 = indexer.put(50, age50);
        assertThat(indexer.isRemovable()).isFalse();
        assertThat(forEachToTuples(indexer, 60)).containsExactlyInAnyOrder(age40, age50);

        indexer.remove(40, entry40);
        assertThat(forEachToTuples(indexer, 60)).containsExactlyInAnyOrder(age50);
        assertThat(indexer.size(60)).isEqualTo(1);

        indexer.remove(50, entry50);
        assertThat(indexer.isRemovable()).isTrue();
        assertThat(forEachToTuples(indexer, 60)).isEmpty();
    }

    @Test
    void treeifiesPastArrayThreshold() {
        // Below-threshold storage is a sorted array; crossing ARRAY_THRESHOLD must treeify without
        // changing LESS_THAN match sets or order (see ScalingNavigableMap's arrayBased/treeify()).
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanAge).buildIndexer(true);
        var threshold = ScalingNavigableMap.ARRAY_THRESHOLD;
        var tuplesByAge = new LinkedHashMap<Integer, UniTuple<String>>();
        for (var age = 0; age <= threshold; age++) { // threshold + 1 puts: crosses the threshold on the last one.
            var tuple = newTuple("age" + age);
            indexer.put(age, tuple);
            tuplesByAge.put(age, tuple);
        }

        churnKey(indexer, -1); // Growth alone no longer treeifies; force it via churn.

        // A few more puts on the tree path, to confirm it keeps working post-treeify.
        for (var age = threshold + 1; age <= threshold + 3; age++) {
            var tuple = newTuple("age" + age);
            indexer.put(age, tuple);
            tuplesByAge.put(age, tuple);
        }

        var queryAge = threshold + 10;
        assertThat(forEachToTuples(indexer, queryAge)).containsExactlyInAnyOrderElementsOf(tuplesByAge.values());
        assertThat(indexer.size(queryAge)).isEqualTo(tuplesByAge.size());

        var midAge = threshold / 2;
        var expectedBelowMid = tuplesByAge.entrySet().stream()
                .filter(e -> e.getKey() < midAge)
                .map(Map.Entry::getValue)
                .toList();
        assertThat(forEachToTuples(indexer, midAge)).containsExactlyInAnyOrderElementsOf(expectedBelowMid);
        assertThat(indexer.size(midAge)).isEqualTo(expectedBelowMid.size());
    }

    @Test
    void treeifyIsOneWayNoDemotionOnRemove() {
        // Once treeified, removing back below ARRAY_THRESHOLD must still behave correctly.
        // (The one-way-ness of the underlying switch is verified directly in ScalingNavigableMapTest.)
        // Constructed directly (rather than via IndexerFactory) to reach comparisonMap.arrayBased below:
        // filling 0..threshold then removing keys 1..threshold only arms the churn counter once
        // (only the first removal leaves size >= threshold; every later one is already below it),
        // so real interleaved churn is needed here to actually force and verify tree mode.
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        var threshold = ScalingNavigableMap.ARRAY_THRESHOLD;
        var entriesByAge = new LinkedHashMap<Integer, ListEntry<UniTuple<String>>>();
        for (var age = 0; age <= threshold; age++) { // threshold + 1 puts: crosses the threshold on the last one.
            entriesByAge.put(age, indexer.put(age, newTuple("age" + age)));
        }

        churnKey(indexer, -1); // Growth alone no longer treeifies; force it via churn.
        assertThat(indexer.comparisonMap.isArrayBased()).isFalse();

        // Remove all but one entry, well below the array threshold.
        entriesByAge.entrySet().stream()
                .filter(e -> e.getKey() > 0)
                .forEach(e -> indexer.remove(e.getKey(), e.getValue()));

        assertThat(indexer.size(threshold + 10)).isEqualTo(1);
        assertThat(indexer.comparisonMap.isArrayBased()).isFalse();
    }

    @Test
    void monotonicGrowthStaysArrayBased() {
        // Regression test: growth alone must not treeify (only churn at/above ARRAY_THRESHOLD does).
        var ageCount = ScalingNavigableMap.ARRAY_THRESHOLD * 4;
        var lessThanIndexer =
                new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                        RandomAccessLeafIndexer::new);
        var greaterThanIndexer =
                new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.GREATER_THAN, KeyUnpacker.<Integer> single(),
                        RandomAccessLeafIndexer::new);
        var tuplesByAge = new LinkedHashMap<Integer, UniTuple<String>>();
        for (var age = 0; age < ageCount; age++) {
            var tuple = newTuple("age" + age);
            lessThanIndexer.put(age, tuple);
            greaterThanIndexer.put(age, tuple);
            tuplesByAge.put(age, tuple);
        }
        assertThat(lessThanIndexer.comparisonMap.isArrayBased()).isTrue();
        assertThat(greaterThanIndexer.comparisonMap.isArrayBased()).isTrue();

        var midAge = ageCount / 2;
        var expectedBelowMid = tuplesByAge.entrySet().stream()
                .filter(e -> e.getKey() < midAge)
                .map(Map.Entry::getValue)
                .toList();
        var expectedAboveMid = tuplesByAge.entrySet().stream()
                .filter(e -> e.getKey() > midAge)
                .map(Map.Entry::getValue)
                .toList();
        assertThat(forEachToTuples(lessThanIndexer, midAge)).containsExactlyInAnyOrderElementsOf(expectedBelowMid);
        assertThat(forEachToTuples(greaterThanIndexer, midAge)).containsExactlyInAnyOrderElementsOf(expectedAboveMid);
    }

    /**
     * Forces a distinct comparisonMap key through CHURN_TOLERANCE put+remove cycles at {@code age},
     * so it gets counted as churn at scale (the fill loop preceding this call must already have
     * brought the indexer to size >= ARRAY_THRESHOLD). {@code age} must not otherwise be used by the
     * calling test: this leaves nothing behind, since every cycle ends with a remove.
     */
    private static void churnKey(Indexer<UniTuple<String>> indexer, int age) {
        for (var i = 0; i < ScalingNavigableMap.CHURN_TOLERANCE; i++) {
            var entry = indexer.put(age, newTuple("churn" + age));
            indexer.remove(age, entry);
        }
    }

    @Test
    void boundaryComparisonHandlesExtremeCompareToWithoutOverflow() {
        Indexer<UniTuple<String>> indexer =
                new ComparisonIndexer<>(JoinerType.GREATER_THAN, KeyUnpacker.<ExtremeKey> single(),
                        RandomAccessLeafIndexer::new);
        var low = new ExtremeKey(1);
        var high = new ExtremeKey(2); // low.compareTo(high) == Integer.MIN_VALUE.
        indexer.put(low, newTuple("low"));

        // low is not greater than high, nor than itself: neither query should match.
        assertThat(forEachToTuples(indexer, high)).isEmpty();
        assertThat(indexer.size(high)).isZero();
        assertThat(forEachToTuples(indexer, low)).isEmpty();
        assertThat(indexer.size(low)).isZero();
    }

    @Test
    void uniqueRandomIteratorDrainsEachMatchExactlyOnce() {
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        indexer.put(10, newTuple("age10a"));
        indexer.put(10, newTuple("age10b"));
        indexer.put(20, newTuple("age20"));
        indexer.put(30, newTuple("age30"));

        assertUniqueRandomDrainMatchesForEach(indexer, 40);
    }

    @Test
    void uniqueRandomIteratorIncludesBoundaryBucket() {
        // LESS_THAN_OR_EQUAL, queried with a key equal to a bucket:
        // forEach and the drain must agree on including that bucket,
        // since both share the same boundaryReached() walk.
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN_OR_EQUAL,
                KeyUnpacker.<Integer> single(), RandomAccessLeafIndexer::new);
        indexer.put(10, newTuple("age10"));
        indexer.put(20, newTuple("age20a"));
        indexer.put(20, newTuple("age20b"));
        indexer.put(30, newTuple("age30"));

        assertUniqueRandomDrainMatchesForEach(indexer, 20);
    }

    @Test
    void uniqueRandomIteratorTreeMode() {
        // advanceFromTree() is a separate code path from advanceFromArray();
        // force treeification first.
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        var threshold = ScalingNavigableMap.ARRAY_THRESHOLD;
        for (var age = 0; age <= threshold; age++) {
            indexer.put(age, newTuple("age" + age));
        }
        churnKey(indexer, -1);
        assertThat(indexer.comparisonMap.isArrayBased()).isFalse();

        assertUniqueRandomDrainMatchesForEach(indexer, threshold + 10);
    }

    @Test
    void uniqueRandomIteratorSingleBucketMap() {
        // A comparisonMap of exactly one bucket bypasses RandomIterator entirely
        // (singleIndexerUniqueIterator returns the downstream iterator directly).
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        indexer.put(10, newTuple("age10a"));
        indexer.put(10, newTuple("age10b"));

        assertUniqueRandomDrainMatchesForEach(indexer, 20); // In range: the only bucket.

        var excludedIterator = indexer.uniqueRandomIterator(5, new Random(0)); // Boundary excludes the only bucket.
        assertThat(excludedIterator.hasNext()).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(excludedIterator::next);
    }

    @Test
    void uniqueRandomIteratorEmptyRange() {
        // comparisonMap.size() > 1, but the boundary excludes every bucket.
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        indexer.put(10, newTuple("age10"));
        indexer.put(20, newTuple("age20"));

        var iterator = indexer.uniqueRandomIterator(5, new Random(0));
        assertThat(iterator.hasNext()).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(iterator::next);
    }

    @Test
    void randomIteratorEmptyRange() {
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        indexer.put(10, newTuple("age10"));
        indexer.put(20, newTuple("age20"));

        var iterator = indexer.randomIterator(5, new Random(0));
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void uniqueRandomIteratorFirstDrawIsNotBiasedToFirstBucket() {
        var indexer = new ComparisonIndexer<UniTuple<String>, Integer>(JoinerType.LESS_THAN, KeyUnpacker.<Integer> single(),
                RandomAccessLeafIndexer::new);
        var age10 = newTuple("age10");
        indexer.put(10, age10);
        var age20 = newTuple("age20");
        indexer.put(20, age20);

        // Seeding every draw straight off an increasing int (new Random(0), new Random(1), ...) will not do:
        // java.util.Random's first nextInt(2) call is constant across such small, close seeds (an LCG artifact),
        // so it could never surface a first-bucket bias regardless of whether one exists. Derive each seed from
        // one root random's nextLong() instead, exactly as AbstractBiasIT.splitFrom does.
        var root = new Random(0);
        var firstDraws = new HashSet<UniTuple<String>>();
        for (var trial = 0; trial < 20; trial++) {
            var iterator = indexer.uniqueRandomIterator(30, new Random(root.nextLong()));
            firstDraws.add(iterator.next());
        }
        assertThat(firstDraws).containsExactlyInAnyOrder(age10, age20);
    }

    private static UniTuple<String> newTuple(String factA) {
        return UniTuple.of(factA, 0);
    }

    private record ExtremeKey(int value) implements Comparable<ExtremeKey> {

        @Override
        public int compareTo(ExtremeKey other) {
            if (value == other.value) {
                return 0;
            }
            // Deliberately extreme instead of a bounded difference,
            // to exercise the sign-flip for GT/GTE without relying on subtraction-based compareTo() tricks elsewhere.
            return value < other.value ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
    }

    private static DefaultBiJoiner<TestPerson, TestPerson> twoComparisons() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.lessThan(TestPerson::age)
                .and(Joiners.greaterThan(TestPerson::age));
    }

    private static DefaultBiJoiner<TestPerson, TestPerson> equalGender() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender);
    }

    private static DefaultBiJoiner<TestPerson, TestPerson> equalThenLessThan() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender)
                .and(Joiners.lessThan(TestPerson::age));
    }

}
