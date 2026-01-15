package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class ContainIndexerTest extends AbstractIndexerTest {

    record TestWorker(String name, List<String> skills, String department, String affinity) {
    }

    record TestJob(String department, String skill, List<String> affinities) {
    }

    private final DefaultBiJoiner<TestWorker, TestJob> joiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.contain(TestWorker::skills, TestJob::skill)
                    .and(Joiners.equal(TestWorker::department, TestJob::department));

    @Test
    void isEmpty() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(indexer.isEmpty()).isTrue();
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
        var annEntry = indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), new UniTuple<>("Ann", 0));

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
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.contain(TestWorker::skills, TestJob::skill)
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

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
