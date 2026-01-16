package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class ContainedInIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<TestJob, TestWorker> joiner =
            (DefaultBiJoiner<TestJob, TestWorker>) Joiners.containedIn(TestJob::skill, TestWorker::skills)
                    .and(Joiners.equal(TestJob::department, TestWorker::department));

    @Test
    void isRemovable() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        assertThat(indexer.isRemovable()).isTrue();
    }

    @Test
    void size() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        assertSize(indexer, List.of("X"), "1").isEqualTo(0);

        putTuple(indexer, "X", "1");

        assertSize(indexer, List.of("X"), "1").isEqualTo(1);
        assertSize(indexer, List.of("X", "Y"), "1").isEqualTo(1);
        assertSize(indexer, List.of("X", "AAA"), "1").isEqualTo(1);
        assertSize(indexer, List.of("Y"), "1").isEqualTo(0);
        assertSize(indexer, List.of("Y", "AAA"), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "999").isEqualTo(0);

        putTuple(indexer, "Y", "1");
        putTuple(indexer, "X", "2");

        assertSize(indexer, List.of("X"), "1").isEqualTo(1);
        assertSize(indexer, List.of("X", "Y"), "1").isEqualTo(2);
        assertSize(indexer, List.of("X", "AAA"), "1").isEqualTo(1);
        assertSize(indexer, List.of("Y"), "1").isEqualTo(1);
        assertSize(indexer, List.of("AAA"), "1").isEqualTo(0);
        assertSize(indexer, List.of("X"), "999").isEqualTo(0);

        putTuple(indexer, null, "1");

        assertSize(indexer, List.of(), "1").isEqualTo(0);
        assertSize(indexer, Collections.singletonList((String) null), "1").isEqualTo(1);
        assertSize(indexer, List.of("X"), "1").isEqualTo(1);
    }

    @Test
    void removeTwice() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var annEntry = indexer.put(CompositeKey.ofMany("X", "1"), UniTuple.of("Ann", 0));

        indexer.remove(CompositeKey.ofMany("X", "1"), annEntry);
        assertThatThrownBy(() -> indexer.remove(CompositeKey.ofMany("X", "1"), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forEach() {
        var indexer = new IndexerFactory<>(joiner).buildIndexer(true);

        var annX1 = putTuple(indexer, "X", "1");
        var bethY1 = putTuple(indexer, "Y", "1");
        var carlX2 = putTuple(indexer, "X", "2");
        var ednaX1 = putTuple(indexer, "X", "1");
        var zeroNull1 = putTuple(indexer, null, "1");

        assertForEach(indexer, List.of("X"), "1").containsExactlyInAnyOrder(annX1, ednaX1);
        assertForEach(indexer, List.of("X", "Y"), "1").containsExactlyInAnyOrder(annX1, bethY1, ednaX1);
        assertForEach(indexer, List.of("Y"), "1").containsExactlyInAnyOrder(bethY1);
        assertForEach(indexer, List.of("Y", "AAA"), "1").containsExactlyInAnyOrder(bethY1);
        assertForEach(indexer, List.of("AAA"), "1").isEmpty();
        assertForEach(indexer, List.of("X"), "999").isEmpty();

        assertForEach(indexer, List.of(), "1").isEmpty();
        assertForEach(indexer, List.of(), "999").isEmpty();
    }

    private final DefaultBiJoiner<TestJob, TestWorker> containComboJoiner =
            (DefaultBiJoiner<TestJob, TestWorker>) Joiners.containedIn(TestJob::skill, TestWorker::skills)
                    .and(Joiners.containing(TestJob::affinities, TestWorker::affinity));

    @Test
    void forEach_containCombo() {
        var indexer = new IndexerFactory<>(containComboJoiner).buildIndexer(true);

        var annX12 = putTuple(indexer, "X", List.of("1", "2"));
        var bethY13 = putTuple(indexer, "Y", List.of("1", "2"));
        var ednaX23 = putTuple(indexer, "X", List.of("2", "3"));

        assertForEach(indexer, List.of("X"), "1").containsExactlyInAnyOrder(annX12);
        assertForEach(indexer, List.of("X", "Y"), "1").containsExactlyInAnyOrder(annX12, bethY13);
        assertForEach(indexer, List.of("X", "Y"), "2").containsExactlyInAnyOrder(annX12, bethY13, ednaX23);

        assertForEach(indexer, List.of(), "1").isEmpty();
    }

    record TestWorker(String name, List<String> skills, String department, String affinity) {
    }

    record TestJob(String department, String skill, List<String> affinities) {
    }

}
