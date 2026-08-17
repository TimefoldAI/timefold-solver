package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;

import org.junit.jupiter.api.Test;

class ContainingAnyOfIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<TestWorker, TestJob> singleJoiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.containingAnyOf(TestWorker::skills, TestJob::skills);

    private final DefaultBiNeighborhoodsJoiner<TestWorker, TestJob> randomAccessSingleJoiner =
            new DefaultBiNeighborhoodsJoiner<>(TestWorker::skills, JoinerType.CONTAINING_ANY_OF, TestJob::skills);

    private final DefaultBiNeighborhoodsJoiner<TestWorker, TestJob> randomAccessMultiJoiner =
            new DefaultBiNeighborhoodsJoiner<>(TestWorker::skills, JoinerType.CONTAINING_ANY_OF,
                    TestJob::skills)
                    .and(NeighborhoodsJoiners.equal(TestWorker::department, TestJob::department));

    private final DefaultBiJoiner<TestWorker, TestJob> multiJoiner =
            singleJoiner.and(Joiners.equal(TestWorker::department, TestJob::department));

    @Test
    void isRemovable() {
        var indexer = new IndexerFactory<>(multiJoiner).buildIndexer(true);

        assertThat(indexer.isRemovable()).isTrue();

        putTuple(indexer, List.of(), "1");

        assertThat(indexer.isRemovable()).isFalse();
    }

    @Test
    void size() {
        var indexer = new IndexerFactory<>(multiJoiner).buildIndexer(true);

        assertSize(indexer, List.of(), "1").isEqualTo(0);
        assertSize(indexer, List.of("X", "Y"), "1").isEqualTo(0);

        putTuple(indexer, List.of("X", "Y"), "1");

        assertSize(indexer, List.of(), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "1").isEqualTo(1);
        assertSize(indexer, List.of("X", "AAA"), "1").isEqualTo(1);
        assertSize(indexer, List.of("Y"), "1").isEqualTo(1);
        assertSize(indexer, List.of("X", "Y"), "1").isEqualTo(1);
        assertSize(indexer, List.of("AAA"), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "999").isEqualTo(0);

        putTuple(indexer, List.of("X", "Z"), "1");
        putTuple(indexer, List.of("X", "Y"), "2");

        assertSize(indexer, List.of(), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "1").isEqualTo(2);
        assertSize(indexer, List.of("X", "AAA"), "1").isEqualTo(2);
        assertSize(indexer, List.of("X", "Y"), "1").isEqualTo(2);
        assertSize(indexer, List.of("X", "Y", "Z"), "1").isEqualTo(2);
        assertSize(indexer, List.of("Y"), "1").isEqualTo(1);
        assertSize(indexer, List.of("Z"), "1").isEqualTo(1);
        assertSize(indexer, List.of("AAA"), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "999").isEqualTo(0);

        putTuple(indexer, List.of(), "1");

        assertSize(indexer, List.of(), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "1").isEqualTo(2);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(multiJoiner).buildIndexer(true);
        var annEntry = indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), UniTuple.of("Ann", 0));

        indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removeLastTupleThenReuseKeys() {
        var indexer = new IndexerFactory<>(multiJoiner).buildIndexer(true);
        var annEntry = indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), UniTuple.of("Ann", 0));

        indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry);
        assertSize(indexer, List.of("X"), "1").isEqualTo(0);
        assertSize(indexer, List.of("Y"), "1").isEqualTo(0);
        assertThat(indexer.isRemovable()).isTrue();

        // The keys are usable again after their downstream indexers were dropped.
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), UniTuple.of("Beth", 0));
        assertSize(indexer, List.of("X"), "1").isEqualTo(1);
        assertSize(indexer, List.of("Y"), "1").isEqualTo(1);
    }

    @Test
    void forEach() {
        var indexer = new IndexerFactory<>(multiJoiner).buildIndexer(true);

        var annXY1 = putTuple(indexer, List.of("X", "Y"), "1");
        var bethXZ1 = putTuple(indexer, List.of("X", "Z"), "1");
        var carlXY2 = putTuple(indexer, List.of("X", "Y"), "2");
        @SuppressWarnings("unused")
        var zero1 = putTuple(indexer, List.of(), "1");

        assertForEach(indexer, List.of("X"), "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertForEach(indexer, List.of("X"), "2").containsExactlyInAnyOrder(carlXY2);
        assertForEach(indexer, List.of("Y"), "2").containsExactlyInAnyOrder(carlXY2);
        assertForEach(indexer, List.of("X", "AAA"), "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertForEach(indexer, List.of("Y"), "1").containsExactlyInAnyOrder(annXY1);
        assertForEach(indexer, List.of("Z"), "1").containsExactlyInAnyOrder(bethXZ1);
        assertForEach(indexer, List.of("X", "Y"), "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertForEach(indexer, List.of("X", "Z"), "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertForEach(indexer, List.of("X", "Y", "Z"), "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertForEach(indexer, List.of("AAA"), "1").isEmpty();
        assertForEach(indexer, List.of("X"), "999").isEmpty();

        assertForEach(indexer, List.of(), "1").isEmpty();
        assertForEach(indexer, List.of(), "999").isEmpty();
    }

    @Test
    void forEachDuplicates() {
        var indexer = new IndexerFactory<>(singleJoiner).buildIndexer(true);
        var key = List.of("X");

        var duplicate = putContainingIndexer(indexer, key);
        indexer.put(key, duplicate);
        var afterDuplicate = putContainingIndexer(indexer, key);

        assertForEach(indexer, List.of("X", "Y")).containsExactlyInAnyOrder(duplicate, afterDuplicate);
    }

    @Test
    void uniqueRandomIterator() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        var annXY1 = putContainingIndexer(indexer, List.of("X", "Y"));
        var bethXZ1 = putContainingIndexer(indexer, List.of("X", "Z"));
        var carlXY2 = putContainingIndexer(indexer, List.of("X", "Y"));
        var zero1 = putContainingIndexer(indexer, List.of());

        assertThat(uniqueRandomIterableForCollectionQuery(indexer, "X"))
                .containsExactlyInAnyOrder(annXY1, bethXZ1, carlXY2);
        assertThat(uniqueRandomIterableForCollectionQuery(indexer, "Y"))
                .containsExactlyInAnyOrder(annXY1, carlXY2);
        assertThat(uniqueRandomIterableForCollectionQuery(indexer, "Z"))
                .containsExactlyInAnyOrder(bethXZ1);

        var list1 = uniqueRandomListForCollectionQuery(indexer, 0, "X");
        var list2 = uniqueRandomListForCollectionQuery(indexer, 2, "X");
        assertThat(list1).containsExactlyInAnyOrderElementsOf(list2);
        assertThat(list1).isNotEqualTo(list2);
    }

    @Test
    void uniqueRandomIteratorDedupesAcrossBuckets() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        putContainingIndexer(indexer, List.of("X", "Y")); // Reachable through both the X and the Y bucket.
        putContainingIndexer(indexer, List.of("X", "Z"));
        putContainingIndexer(indexer, List.of("Y"));
        putContainingIndexer(indexer, List.of());

        assertUniqueRandomDrainMatchesForEach(indexer, List.of("X", "Y", "Z"));
    }

    @Test
    void uniqueRandomIteratorSingleDrawNeedsNoDedupe() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        var annXY = putContainingIndexer(indexer, List.of("X", "Y"));
        var bethXZ = putContainingIndexer(indexer, List.of("X", "Z"));
        var carlY = putContainingIndexer(indexer, List.of("Y"));

        // A single draw must never need to allocate removedSet to be correct.
        var iterator = indexer.uniqueRandomIterator(List.of("X", "Y", "Z"), new Random(0));
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isIn(annXY, bethXZ, carlY);
    }

    @Test
    void uniqueRandomIteratorDuplicateQueryKeys() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        putContainingIndexer(indexer, List.of("X", "Y"));
        putContainingIndexer(indexer, List.of("X", "Z"));

        // Same bucket entered twice via a duplicated query key;
        // DefaultIterator's distinctingSet already drops the repeat while draining to the distinct list,
        // so every match still comes out exactly once.
        assertUniqueRandomDrainMatchesForEach(indexer, List.of("X", "X"));
    }

    @Test
    void uniqueRandomIteratorUnmatchedQueryKey() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);
        putContainingIndexer(indexer, List.of("X", "Y"));

        // A key with zero matches must not throw; it becomes a dead bucket of weight 0.
        assertUniqueRandomDrainMatchesForEach(indexer, List.of("X", "Q"));

        var noMatchIterator = indexer.uniqueRandomIterator(List.of("Q"), new Random(0));
        assertThat(noMatchIterator.hasNext()).isFalse();

        var emptyIndexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);
        var emptyIterator = emptyIndexer.uniqueRandomIterator(List.of("X"), new Random(0));
        assertThat(emptyIterator.hasNext()).isFalse();
    }

    @Test
    void uniqueRandomIteratorCompositeJoiner() {
        var indexer = new IndexerFactory<>(randomAccessMultiJoiner).buildIndexer(true);

        putTuple(indexer, List.of("X", "Y"), "1");
        putTuple(indexer, List.of("X", "Z"), "1");
        // Different department: must not inflate department "1"'s weight, and size(queryCompositeKey)
        // must be filtered by department, not just by the raw "X"/"Y" bucket.
        putTuple(indexer, List.of("X", "Y"), "2");

        assertUniqueRandomDrainMatchesForEach(indexer, CompositeKey.ofMany(List.of("X", "Y"), "1"));
    }

    @Test
    void randomIteratorNeverEnds() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        putContainingIndexer(indexer, List.of("X", "Y"));
        putContainingIndexer(indexer, List.of("X", "Z"));
        putContainingIndexer(indexer, List.of("Y"));

        assertRepeatingRandomNeverEnds(indexer, List.of("X", "Y", "Z"), 30);

        // A non-empty query key collection whose every key is unmatched is the one shape where
        // a repeating iterator is legitimately born dead.
        var deadIterator = indexer.randomIterator(List.of("Q"), new Random(0));
        assertThat(deadIterator.hasNext()).isFalse();
    }

    private record TestWorker(String name, List<String> skills, String department) {
    }

    private record TestJob(String department, List<String> skills) {
    }

}
