package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * Covers the NON-unified (two parallel indexer) path: joins whose indexing joiners contain no equal joiner,
 * so {@link IndexerFactory#isFusedEqualIndexEligible()} is false and the node keeps two comparison indexers.
 * Comparison is otherwise only exercised alongside a leading equal (see {@link EqualsAndComparisonIndexerTest});
 * a pure-comparison join takes the {@code useJoinIndex == false} branch and must still join correctly.
 * <p>
 * A single comparison joiner uses a {@link KeyUnpacker#single()},
 * so the index key is the raw (comparable) key, not a {@link CompositeKey};
 * the tests put/query with raw keys accordingly.
 */
class ComparisonIndexerTest extends AbstractIndexerTest {

    @SuppressWarnings("unchecked")
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
    void randomIteratorIsFairAcrossLeafIndexersOfDifferentSizes() {
        var indexer = newRandomAccessLessThanIndexer();
        for (var i = 0; i < 100; i++) {
            indexer.put(10, newTuple("age10-" + i));
        }
        var age20 = newTuple("age20");
        indexer.put(20, age20);
        var age30 = newTuple("age30");
        indexer.put(30, age30);
        var age40 = newTuple("age40");
        indexer.put(40, age40);

        var random = new Random(0);
        var iterations = 200_000;
        var selectionCountMap = new HashMap<UniTuple<String>, Integer>();
        for (var i = 0; i < iterations; i++) {
            var iterator = indexer.randomIterator(100, random);
            assertThat(iterator).hasNext();
            selectionCountMap.merge(iterator.next(), 1, Integer::sum);
        }

        var expectedTinyCount = iterations / 103;
        assertThat(selectionCountMap.getOrDefault(age20, 0)).isBetween(expectedTinyCount / 2, expectedTinyCount * 2);
        assertThat(selectionCountMap.getOrDefault(age30, 0)).isBetween(expectedTinyCount / 2, expectedTinyCount * 2);
        assertThat(selectionCountMap.getOrDefault(age40, 0)).isBetween(expectedTinyCount / 2, expectedTinyCount * 2);
        var tinyBucketSelectionCount = selectionCountMap.getOrDefault(age20, 0)
                + selectionCountMap.getOrDefault(age30, 0)
                + selectionCountMap.getOrDefault(age40, 0);
        var bigBucketShare = (iterations - tinyBucketSelectionCount) / (double) iterations;
        assertThat(bigBucketShare).isBetween(0.93, 0.99);
    }

    @Test
    void randomIteratorSingleBucketDelegatesToLeaf() {
        var indexer = newRandomAccessLessThanIndexer();
        var age10 = newTuple("age10");
        indexer.put(10, age10);
        var age10b = newTuple("age10b");
        indexer.put(10, age10b);
        var age10c = newTuple("age10c");
        indexer.put(10, age10c);

        var iterator = indexer.randomIterator(100, new Random(0));
        var resultList = new ArrayList<UniTuple<String>>();
        while (iterator.hasNext()) {
            resultList.add(iterator.next());
            iterator.remove();
        }

        assertThat(resultList).containsExactlyInAnyOrder(age10, age10b, age10c);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void randomIteratorSkipsOutOfRangeBuckets() {
        var indexer = newRandomAccessLessThanIndexer();
        indexer.put(10, newTuple("age10"));
        indexer.put(20, newTuple("age20"));

        var iterator = indexer.randomIterator(5, new Random(0));

        assertThat(iterator.hasNext()).isFalse();
        assertThatThrownBy(iterator::next)
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void randomIteratorRemoveBeforeNextThrows() {
        var indexer = newRandomAccessLessThanIndexer();
        indexer.put(10, newTuple("age10"));
        indexer.put(20, newTuple("age20"));

        var iterator = indexer.randomIterator(100, new Random(0));

        assertThatThrownBy(iterator::remove)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void randomIteratorWithFilterRespectsPredicateAndIsComplete() {
        var indexer = newRandomAccessLessThanIndexer();
        var age10 = newTuple("age10");
        indexer.put(10, age10);
        var age10b = newTuple("age10b");
        indexer.put(10, age10b);
        var age10c = newTuple("age10c");
        indexer.put(10, age10c);
        var age10d = newTuple("age10d");
        indexer.put(10, age10d);
        var age10e = newTuple("age10e");
        indexer.put(10, age10e);
        var age20 = newTuple("age20");
        indexer.put(20, age20);
        var age20b = newTuple("age20b");
        indexer.put(20, age20b);
        var age20c = newTuple("age20c");
        indexer.put(20, age20c);
        var age20d = newTuple("age20d");
        indexer.put(20, age20d);
        var age20e = newTuple("age20e");
        indexer.put(20, age20e);

        var allowedSet = Set.of(age10, age10d, age20b, age20e);
        var iterator = indexer.randomIterator(100, new Random(0), allowedSet::contains);
        var resultList = new ArrayList<UniTuple<String>>();
        while (iterator.hasNext()) {
            resultList.add(iterator.next());
            iterator.remove();
        }

        assertThat(resultList).containsExactlyInAnyOrder(age10, age10d, age20b, age20e);
    }

    private static ComparisonIndexer<UniTuple<String>, Integer> newRandomAccessLessThanIndexer() {
        return new ComparisonIndexer<>(JoinerType.LESS_THAN, KeyUnpacker.<Integer>single(),
                RandomAccessLeafIndexer::new);
    }

    private static UniTuple<String> newTuple(String factA) {
        return UniTuple.of(factA, 0);
    }

    @SuppressWarnings("unchecked")
    private static DefaultBiJoiner<TestPerson, TestPerson> twoComparisons() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.lessThan(TestPerson::age)
                .and(Joiners.greaterThan(TestPerson::age));
    }

    @SuppressWarnings("unchecked")
    private static DefaultBiJoiner<TestPerson, TestPerson> equalGender() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender);
    }

    @SuppressWarnings("unchecked")
    private static DefaultBiJoiner<TestPerson, TestPerson> equalThenLessThan() {
        return (DefaultBiJoiner<TestPerson, TestPerson>) Joiners.equal(TestPerson::gender)
                .and(Joiners.lessThan(TestPerson::age));
    }

}
