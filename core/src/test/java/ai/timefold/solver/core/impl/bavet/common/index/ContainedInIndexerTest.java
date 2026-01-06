package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class ContainedInIndexerTest extends AbstractIndexerTest {

    record TestWorker(String name, List<String> skills, String department, String affinity) {
    }

    record TestJob(String department, String skill, List<String> affinities) {
    }

    private final DefaultBiJoiner<TestJob, TestWorker> joiner =
            (DefaultBiJoiner<TestJob, TestWorker>) Joiners.containedIn(TestJob::skill, TestWorker::skills)
                    .and(Joiners.equal(TestJob::department, TestWorker::department));

    @Test
    void isEmpty() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(indexer.isEmpty()).isTrue();
    }

    @Test
    void size() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "1"))).isEqualTo(0);

        indexer.put(CompositeKey.ofMany("X", "1"), newTuple("Ann"));
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y"), "1"))).isEqualTo(0);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y", "Z"), "1"))).isEqualTo(0);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "3"))).isEqualTo(0);

        indexer.put(CompositeKey.ofMany("Y", "1"), newTuple("Beth"));
        indexer.put(CompositeKey.ofMany("X", "2"), newTuple("Carl"));
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y", "Z"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "3"))).isEqualTo(0);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annTuple = newTuple("Ann");
        var annEntry = indexer.put(CompositeKey.ofMany("X", "1"), annTuple);

        indexer.remove(CompositeKey.ofMany("X", "1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany("X", "1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forEach() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        var annX1 = newTuple("Ann");
        indexer.put(CompositeKey.ofMany("X", "1"), annX1);
        var bethY1 = newTuple("Beth");
        indexer.put(CompositeKey.ofMany("Y", "1"), bethY1);
        indexer.put(CompositeKey.ofMany("X", "2"), newTuple("Carl"));
        indexer.put(CompositeKey.ofMany("Z", "3"), newTuple("Dan"));
        var ednaX1 = newTuple("Edna");
        indexer.put(CompositeKey.ofMany("X", "1"), ednaX1);

        assertThat(forEachToTuples(indexer, List.of("X"), "1")).containsExactlyInAnyOrder(annX1, ednaX1);
        assertThat(forEachToTuples(indexer, List.of("X", "Y"), "1")).containsExactlyInAnyOrder(annX1, bethY1, ednaX1);
        assertThat(forEachToTuples(indexer, List.of("Y"), "1")).containsExactlyInAnyOrder(bethY1);
        assertThat(forEachToTuples(indexer, List.of("Y", "W"), "1")).containsExactlyInAnyOrder(bethY1);
        assertThat(forEachToTuples(indexer, List.of("W"), "1")).isEmpty();
        assertThat(forEachToTuples(indexer, List.of(), "1")).isEmpty();
        assertThat(forEachToTuples(indexer, List.of("X"), "3")).isEmpty();
    }

    private final DefaultBiJoiner<TestJob, TestWorker> containComboJoiner =
            (DefaultBiJoiner<TestJob, TestWorker>) Joiners.containedIn(TestJob::skill, TestWorker::skills)
                    .and(Joiners.contain(TestJob::affinities, TestWorker::affinity));

    @Test
    void forEach_containCombo() {
        var indexer = new IndexerFactory<>(containComboJoiner).buildIndexer(true);

        var annX12 = newTuple("Ann");
        indexer.put(CompositeKey.ofMany("X", List.of("1", "2")), annX12);
        var bethY13 = newTuple("Beth");
        indexer.put(CompositeKey.ofMany("Y", List.of("1", "2")), bethY13);
        var ednaX23 = newTuple("Edna");
        indexer.put(CompositeKey.ofMany("X", List.of("2", "3")), ednaX23);

        assertThat(forEachToTuples(indexer, List.of("X"), "1")).containsExactlyInAnyOrder(annX12);
        assertThat(forEachToTuples(indexer, List.of("X", "Y"), "1")).containsExactlyInAnyOrder(annX12, bethY13);
        assertThat(forEachToTuples(indexer, List.of("X", "Y"), "2")).containsExactlyInAnyOrder(annX12, bethY13, ednaX23);
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
