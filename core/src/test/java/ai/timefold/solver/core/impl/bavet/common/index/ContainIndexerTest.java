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
        assertThat(indexer.size(CompositeKey.ofMany("X", "1"))).isEqualTo(0);

        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), newTuple("Ann"));
        assertThat(indexer.size(CompositeKey.ofMany("X", "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany("Y", "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany("Z", "1"))).isEqualTo(0);
        assertThat(indexer.size(CompositeKey.ofMany("X", "3"))).isEqualTo(0);

        indexer.put(CompositeKey.ofMany(List.of("X", "Z"), "1"), newTuple("Beth"));
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "2"), newTuple("Carl"));
        assertThat(indexer.size(CompositeKey.ofMany("X", "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany("Y", "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany("Z", "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany("X", "3"))).isEqualTo(0);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annTuple = newTuple("Ann");
        var annEntry = indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), annTuple);

        indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forEach() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        var annXY1 = newTuple("Ann");
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), annXY1);
        var bethXZ1 = newTuple("Beth");
        indexer.put(CompositeKey.ofMany(List.of("X", "Z"), "1"), bethXZ1);
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "2"), newTuple("Carl"));
        indexer.put(CompositeKey.ofMany(List.of("X", "Z"), "3"), newTuple("Dan"));
        var ednaYZ1 = newTuple("Edna");
        indexer.put(CompositeKey.ofMany(List.of("Y", "Z"), "1"), ednaYZ1);

        assertThat(forEachToTuples(indexer, "X", "1")).containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertThat(forEachToTuples(indexer, "Y", "1")).containsExactlyInAnyOrder(annXY1, ednaYZ1);
        assertThat(forEachToTuples(indexer, "Z", "1")).containsExactlyInAnyOrder(bethXZ1, ednaYZ1);
        assertThat(forEachToTuples(indexer, "W", "1")).isEmpty();
        assertThat(forEachToTuples(indexer, "Y", "3")).isEmpty();
    }

    private final DefaultBiJoiner<TestWorker, TestJob> containedInComboJoiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.contain(TestWorker::skills, TestJob::skill)
                    .and(Joiners.containedIn(TestWorker::affinity, TestJob::affinities));

    @Test
    void forEach_containedInCombo() {
        var indexer = new IndexerFactory<>(containedInComboJoiner).buildIndexer(true);

        var annXY1 = newTuple("Ann");
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), annXY1);
        var bethXY2 = newTuple("Beth");
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "2"), bethXY2);
        var ednaYZ1 = newTuple("Edna");
        indexer.put(CompositeKey.ofMany(List.of("Y", "Z"), "1"), ednaYZ1);

        assertThat(forEachToTuples(indexer, "X", List.of("1"))).containsExactlyInAnyOrder(annXY1);
        assertThat(forEachToTuples(indexer, "X", List.of("1", "2"))).containsExactlyInAnyOrder(annXY1, bethXY2);
        assertThat(forEachToTuples(indexer, "Y", List.of("1", "2"))).containsExactlyInAnyOrder(annXY1, bethXY2, ednaYZ1);
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
