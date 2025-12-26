package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class ContainIndexerTest extends AbstractIndexerTest {

    record TestWorker(String name, List<String> skills, String department) {
    }

    record TestJob(String department, String skill) {
    }

    private final DefaultBiJoiner<TestWorker, TestJob> joiner =
            (DefaultBiJoiner<TestWorker, TestJob>) Joiners.contain(TestWorker::skills, TestJob::skill)
                    .and(Joiners.equal(TestWorker::department, TestJob::department));

    @Test
    void isEmpty() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(getTuples(indexer, List.of("X", "Y"), "Dep1")).isEmpty();
    }

    @Test
    void put() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annTuple = newTuple("Ann-XY-Dep1");
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"))).isEqualTo(0);
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"), annTuple);
        assertThat(indexer.size(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"))).isEqualTo(1);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annTuple = newTuple("Ann-XY-Dep1");
        var annEntry = indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"), annTuple);

        indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        var annTuple = newTuple("Ann-XY-Dep1");
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "Dep1"), annTuple);
        var bethTuple = newTuple("Beth-XZ-Dep1");
        indexer.put(CompositeKey.ofMany(List.of("X", "Z"), "Dep1"), bethTuple);
        indexer.put(CompositeKey.ofMany(List.of("X", "Y"), "Dep2"), newTuple("Carl-XY-Dep2"));
        indexer.put(CompositeKey.ofMany(List.of("X", "Z"), "Dep3"), newTuple("Dan-XZ-Dep3"));
        var ednaTuple = newTuple("Edna-YZ-Dep1");
        indexer.put(CompositeKey.ofMany(List.of("Y", "Z"), "Dep1"), ednaTuple);

        assertThat(getTuples(indexer, "X", "Dep1")).containsOnly(annTuple, bethTuple);
        assertThat(getTuples(indexer, "Y", "Dep1")).containsOnly(annTuple, ednaTuple);
        assertThat(getTuples(indexer, "Z", "Dep1")).containsOnly(bethTuple, ednaTuple);
        assertThat(getTuples(indexer, "W", "Dep1")).isEmpty();
        assertThat(getTuples(indexer, "Y", "Dep3")).isEmpty();
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
