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
class ContainedInIndexerTest extends AbstractIndexerTest {

    record TestWorker(String name, List<String> skills, String department) {
    }

    record TestJob(String department, String skill) {
    }

    private final DefaultBiJoiner<TestJob, TestWorker> joiner =
            (DefaultBiJoiner<TestJob, TestWorker>) Joiners.containedIn(TestJob::skill, TestWorker::skills)
                    .and(Joiners.equal(TestJob::department, TestWorker::department));

    @Test
    void isEmpty() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(getTuples(indexer, "X", "Dep1")).isEmpty();
    }

    @Test
    void put() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annTuple = newTuple("Ann-X-Dep1");
        assertThat(indexer.size(CompositeKey.ofMany("X", "Dep1"))).isEqualTo(0);
        indexer.put(CompositeKey.ofMany("X", "Dep1"), annTuple);
        assertThat(indexer.size(CompositeKey.ofMany("X", "Dep1"))).isEqualTo(1);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annTuple = newTuple("Ann-X-Dep1");
        var annEntry = indexer.put(CompositeKey.ofMany("X", "Dep1"), annTuple);

        indexer.remove(CompositeKey.ofMany("X", "Dep1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany("X", "Dep1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        var annX1 = newTuple("Ann");
        indexer.put(CompositeKey.ofMany("X", "1"), annX1);
        var bethY1 = newTuple("Beth");
        indexer.put(CompositeKey.ofMany("Y", "1"), bethY1);
        indexer.put(CompositeKey.ofMany("X", "2"), newTuple("Carl"));
        indexer.put(CompositeKey.ofMany("Z", "3"), newTuple("Dan"));
        var ednaX1 = newTuple("Edna");
        indexer.put(CompositeKey.ofMany("X", "1"), ednaX1);

        assertThat(getTuples(indexer, List.of("X"), "1")).containsOnly(annX1, ednaX1);
        assertThat(getTuples(indexer, List.of("X", "Y"), "1")).containsOnly(annX1, bethY1, ednaX1);
        assertThat(getTuples(indexer, List.of("Y"), "1")).containsOnly(bethY1);
        assertThat(getTuples(indexer, List.of("Y", "W"), "1")).containsOnly(bethY1);
        assertThat(getTuples(indexer, List.of("W"), "1")).isEmpty();
        assertThat(getTuples(indexer, List.of(), "1")).isEmpty();
        assertThat(getTuples(indexer, List.of("X"), "3")).isEmpty();
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
