package ai.timefold.solver.core.impl.bavet.common.index;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainAnyIndexerTest extends AbstractIndexerTest {

    record TestWorker(String name, List<String> skills, String department) {
    }

    record TestJob(String department, List<String> skills) {
    }

    private final DefaultBiJoiner<TestWorker, TestJob> joiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.containAny(TestWorker::skills, TestJob::skills)
                    .and(Joiners.equal(TestWorker::department, TestJob::department));

    @Test
    void isEmpty() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(indexer.isEmpty()).isTrue();
    }

    @Test @Disabled
    void size() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "1"))).isEqualTo(0);

        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "1"), newTuple("Ann"));
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "W"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y", "W"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "1"))).isEqualTo(1);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("V", "W"), "1"))).isEqualTo(0);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "3"))).isEqualTo(0);

        indexer.put(CompositeKey.ofMany(List.of("X", "Z"), "1"), newTuple("Beth"));
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "2"), newTuple("Carl"));
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "W"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y", "Z"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Y"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("Z"), "1"))).isEqualTo(2);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("W"), "1"))).isEqualTo(0);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X"), "3"))).isEqualTo(0);
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

    @Test @Disabled
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

        assertThat(forEachToTuples(indexer, List.of("X"), "1")).containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertThat(forEachToTuples(indexer, List.of("X", "W"), "1")).containsExactlyInAnyOrder(annXY1, bethXZ1);
        assertThat(forEachToTuples(indexer, List.of("Y"), "1")).containsExactlyInAnyOrder(annXY1, ednaYZ1);
        assertThat(forEachToTuples(indexer, List.of("Z"), "1")).containsExactlyInAnyOrder(bethXZ1, ednaYZ1);
        assertThat(forEachToTuples(indexer, List.of("X", "Y"), "1")).containsExactlyInAnyOrder(annXY1, bethXZ1, ednaYZ1);
        assertThat(forEachToTuples(indexer, List.of("X", "Z"), "1")).containsExactlyInAnyOrder(annXY1, bethXZ1, ednaYZ1);
        assertThat(forEachToTuples(indexer, List.of("W"), "1")).isEmpty();
        assertThat(forEachToTuples(indexer, List.of("Y"), "3")).isEmpty();
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
