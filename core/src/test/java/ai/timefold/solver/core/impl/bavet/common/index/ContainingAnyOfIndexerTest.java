package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;

import org.junit.jupiter.api.Test;

class ContainingAnyOfIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<TestWorker, TestJob> singleJoiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.containingAnyOf(TestWorker::skills, TestJob::skills);

    private final DefaultBiNeighborhoodsJoiner<TestWorker, TestJob> randomAccessSingleJoiner =
            new DefaultBiNeighborhoodsJoiner<>(TestWorker::skills, JoinerType.CONTAINING_ANY_OF, TestJob::skills);

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
    void forEach() {
        var indexer = new IndexerFactory<>(multiJoiner).buildIndexer(true);

        var annXY1 = putTuple(indexer, List.of("X", "Y"), "1");
        var bethXZ1 = putTuple(indexer, List.of("X", "Z"), "1");
        var carlXY2 = putTuple(indexer, List.of("X", "Y"), "2");
        var zero1 = putTuple(indexer, List.of(), "1");

        assertForEach(indexer, List.of("X"), "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
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

    private UniTuple<String> putSingle(Indexer<Object> indexer, List<String> keys, int id) {
        var tuple = UniTuple.of("Tuple " + id, 0);
        indexer.put(keys, tuple);
        return tuple;
    }

    @Test
    void forEachDuplicates() {
        var indexer = new IndexerFactory<>(singleJoiner).buildIndexer(true);
        var key = List.of("X");

        var duplicate = putSingle(indexer, key, 0);
        indexer.put(key, duplicate);
        var afterDuplicate = putSingle(indexer, key, 1);

        assertForEach(indexer, List.of("X", "Y")).containsExactlyInAnyOrder(duplicate, afterDuplicate);
    }

    private Iterable<Object> randomIterable(Indexer<Object> indexer, String... keys) {
        return () -> {
            var random = new Random(0);
            var delegate = indexer.randomIterator(List.of(keys), random);
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public Object next() {
                    var out = delegate.next();
                    delegate.remove();
                    return out;
                }
            };
        };
    }

    @Test
    void randomIterator() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        var annXY1 = putSingle(indexer, List.of("X", "Y"), 0);
        var bethXZ1 = putSingle(indexer, List.of("X", "Z"), 1);
        var carlXY2 = putSingle(indexer, List.of("X", "Y"), 2);
        var zero1 = putSingle(indexer, List.of(), 3);

        assertThat(randomIterable(indexer, "X"))
                .containsExactlyInAnyOrder(annXY1, bethXZ1, carlXY2);
        assertThat(randomIterable(indexer, "Y"))
                .containsExactlyInAnyOrder(annXY1, carlXY2);
        assertThat(randomIterable(indexer, "Z"))
                .containsExactlyInAnyOrder(bethXZ1);
    }

    private record TestWorker(String name, List<String> skills, String department) {
    }

    private record TestJob(String department, List<String> skills) {
    }

}
