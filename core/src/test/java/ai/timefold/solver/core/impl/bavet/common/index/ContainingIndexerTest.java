package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;

import org.junit.jupiter.api.Test;

class ContainingIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<TestWorker, TestJob> joiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.containing(TestWorker::skills, TestJob::skill)
                    .and(Joiners.equal(TestWorker::department, TestJob::department));

    private final DefaultBiNeighborhoodsJoiner<TestWorker, TestJob> randomAccessSingleJoiner =
            new DefaultBiNeighborhoodsJoiner<>(TestWorker::skills, JoinerType.CONTAINING, TestJob::skill);

    @Test
    void isRemovable() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        assertThat(indexer.isRemovable()).isTrue();

        putTuple(indexer, List.of(), "1");

        assertThat(indexer.isRemovable()).isFalse();
    }

    @Test
    void size() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        assertSize(indexer, "X", "1").isEqualTo(0);

        putTuple(indexer, List.of("X", "Y"), "1");

        assertSize(indexer, "X", "1").isEqualTo(1);
        assertSize(indexer, "Y", "1").isEqualTo(1);
        assertSize(indexer, "Z", "1").isEqualTo(0);
        assertSize(indexer, "X", "999").isEqualTo(0);

        putTuple(indexer, List.of("X", "Z"), "1");
        putTuple(indexer, List.of("X", "Y"), "2");

        assertSize(indexer, "X", "1").isEqualTo(2);
        assertSize(indexer, "Y", "1").isEqualTo(1);
        assertSize(indexer, "Z", "1").isEqualTo(1);
        assertSize(indexer, "AAA", "1").isEqualTo(0);
        assertSize(indexer, "X", "999").isEqualTo(0);

        putTuple(indexer, List.of(), "1");

        assertSize(indexer, null, "1").isEqualTo(0);
        assertSize(indexer, "X", "1").isEqualTo(2);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annEntry = indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), UniTuple.of("Ann", 0));

        indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forEach() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        var annXY1 = putTuple(indexer, List.of("X", "Y"), "1");
        var bethXZ1 = putTuple(indexer, List.of("X", "Z"), "1");
        var carlXY2 = putTuple(indexer, List.of("X", "Y"), "2");
        var ednaYZ1 = putTuple(indexer, List.of("Y", "Z"), "1");
        var zero1 = putTuple(indexer, List.of(), "1");

        assertForEach(indexer, "X", "1").containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertForEach(indexer, "Y", "1").containsExactlyInAnyOrder(annXY1, ednaYZ1);
        assertForEach(indexer, "Z", "1").containsExactlyInAnyOrder(bethXZ1, ednaYZ1);
        assertForEach(indexer, "AAA", "1").isEmpty();
        assertForEach(indexer, "X", "999").isEmpty();

        assertForEach(indexer, null, "1").isEmpty();
        assertForEach(indexer, null, "999").isEmpty();
    }

    private final DefaultBiJoiner<TestWorker, TestJob> containedInComboJoiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.containing(TestWorker::skills, TestJob::skill)
                    .and(Joiners.containedIn(TestWorker::affinity, TestJob::affinities));

    @Test
    void forEach_containedInCombo() {
        var indexer = new IndexerFactory<>(containedInComboJoiner).buildIndexer(true);

        var annXY1 = putTuple(indexer, List.of("X", "Y"), "1");
        var bethXY2 = putTuple(indexer, List.of("X", "Y"), "2");
        var ednaYZ1 = putTuple(indexer, List.of("Y", "Z"), "1");

        assertForEach(indexer, "X", List.of("1")).containsExactlyInAnyOrder(annXY1);
        assertForEach(indexer, "X", List.of("1", "2")).containsExactlyInAnyOrder(annXY1, bethXY2);
        assertForEach(indexer, "Y", List.of("1", "2")).containsExactlyInAnyOrder(annXY1, bethXY2, ednaYZ1);

        assertForEach(indexer, "X", List.of()).isEmpty();
    }

    @Test
    void randomIterator() {
        var indexer = new IndexerFactory<>(randomAccessSingleJoiner).buildIndexer(true);

        var annXY1 = putContainingIndexer(indexer, List.of("X", "Y"));
        var bethXZ1 = putContainingIndexer(indexer, List.of("X", "Z"));
        var carlXY2 = putContainingIndexer(indexer, List.of("X", "Y"));
        var zero1 = putContainingIndexer(indexer, List.of());

        assertThat(randomIterableForQuery(indexer, "X"))
                .containsExactlyInAnyOrder(annXY1, bethXZ1, carlXY2);
        assertThat(randomIterableForQuery(indexer, "Y"))
                .containsExactlyInAnyOrder(annXY1, carlXY2);
        assertThat(randomIterableForQuery(indexer, "Z"))
                .containsExactlyInAnyOrder(bethXZ1);

        var list1 = randomListForQuery(indexer, 0, "X");
        // seed 0 and 1 has the same list, but 2 is different
        var list2 = randomListForQuery(indexer, 2, "X");
        assertThat(list1).containsExactlyInAnyOrderElementsOf(list2);
        assertThat(list1).isNotEqualTo(list2);
    }

    record TestWorker(String name, List<String> skills, String department, String affinity) {
    }

    record TestJob(String department, String skill, List<String> affinities) {
    }

}
