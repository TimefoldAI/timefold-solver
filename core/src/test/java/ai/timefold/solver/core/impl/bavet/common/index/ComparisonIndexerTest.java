package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ListEntry;

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
    void treeifiesPastArrayThreshold() throws Exception {
        // Below-threshold storage is a sorted array; crossing ARRAY_THRESHOLD must treeify without
        // changing LESS_THAN match sets or order (see ComparisonIndexer's belowThreshold/treeify()).
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanAge).buildIndexer(true);
        var threshold = arrayThreshold();
        var tuplesByAge = new LinkedHashMap<Integer, UniTuple<String>>();
        for (var age = 0; age <= threshold; age++) { // threshold + 1 puts: crosses the threshold on the last one.
            var tuple = newTuple("age" + age);
            indexer.put(age, tuple);
            tuplesByAge.put(age, tuple);
        }
        assertThat(isBelowThreshold(indexer)).isFalse();

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
    void treeifyIsOneWayNoDemotionOnRemove() throws Exception {
        // Once treeified, removing back below ARRAY_THRESHOLD must NOT revert to array mode.
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanAge).buildIndexer(true);
        var threshold = arrayThreshold();
        var entriesByAge = new LinkedHashMap<Integer, ListEntry<UniTuple<String>>>();
        for (var age = 0; age <= threshold; age++) { // threshold + 1 puts: crosses the threshold on the last one.
            entriesByAge.put(age, indexer.put(age, newTuple("age" + age)));
        }
        assertThat(isBelowThreshold(indexer)).isFalse();

        // Remove all but one entry, well below the array threshold.
        entriesByAge.entrySet().stream()
                .filter(e -> e.getKey() > 0)
                .forEach(e -> indexer.remove(e.getKey(), e.getValue()));

        assertThat(isBelowThreshold(indexer)).isFalse();
        assertThat(indexer.size(threshold + 10)).isEqualTo(1);
    }

    private static int arrayThreshold() throws Exception {
        var field = ComparisonIndexer.class.getDeclaredField("ARRAY_THRESHOLD");
        field.setAccessible(true);
        return field.getInt(null);
    }

    private static boolean isBelowThreshold(Indexer<?> indexer) throws Exception {
        var field = ComparisonIndexer.class.getDeclaredField("belowThreshold");
        field.setAccessible(true);
        return field.getBoolean(indexer);
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
