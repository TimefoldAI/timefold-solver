package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class ContainAnyIndexerTest extends AbstractIndexerTest {

    record TestWorker(String name, List<String> skills, String department) {
    }

    record TestJob(String department, List<String> skills) {
    }

    private final DefaultBiJoiner<TestWorker, TestJob> joiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.containAny(TestWorker::skills, TestJob::skills)
                    .and(Joiners.equal(TestWorker::department, TestJob::department));

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

}
