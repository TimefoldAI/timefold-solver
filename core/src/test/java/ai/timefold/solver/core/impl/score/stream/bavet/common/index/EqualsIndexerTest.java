package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.bi.DefaultBiJoiner;
import ai.timefold.solver.core.impl.util.Pair;

import org.junit.jupiter.api.Test;

class EqualsIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<Person, Person> joiner =
            (DefaultBiJoiner<Person, Person>) Joiners.equal(Person::gender)
                    .and(Joiners.equal(Person::age));

    @Test
    void isEmpty() {
        var index = new IndexFactory<>(joiner).buildIndex(true);
        assertThat(getTuples(index, "F", 40)).isEmpty();
    }

    @Test
    void put() {
        var index = new IndexFactory<>(joiner).buildIndex(true);
        var annTuple = newTuple("Ann-F-40");
        assertThat(index.size(IndexKeys.ofMany("F", 40))).isEqualTo(0);
        index.put(IndexKeys.ofMany("F", 40), annTuple);
        assertThat(index.size(IndexKeys.ofMany("F", 40))).isEqualTo(1);
    }

    @Test
    void removeTwice() {
        var index = new IndexFactory<>(joiner).buildIndex(true);
        var annTuple = newTuple("Ann-F-40");
        var annEntry = index.put(IndexKeys.ofMany("F", 40), annTuple);

        index.remove(IndexKeys.ofMany("F", 40), annEntry);
        assertThatThrownBy(() -> index.remove(IndexKeys.ofMany("F", 40), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        var index = new IndexFactory<>(joiner).buildIndex(true);

        var annTuple = newTuple("Ann-F-40");
        index.put(IndexKeys.of(new Pair<>("F", 40)), annTuple);
        var bethTuple = newTuple("Beth-F-30");
        index.put(IndexKeys.of(new Pair<>("F", 30)), bethTuple);
        index.put(IndexKeys.of(new Pair<>("M", 40)), newTuple("Carl-M-40"));
        index.put(IndexKeys.of(new Pair<>("M", 30)), newTuple("Dan-M-30"));
        var ednaTuple = newTuple("Edna-F-40");
        index.put(IndexKeys.of(new Pair<>("F", 40)), ednaTuple);

        assertThat(getTuples(index, new Pair<>("F", 40))).containsOnly(annTuple, ednaTuple);
        assertThat(getTuples(index, new Pair<>("F", 30))).containsOnly(bethTuple);
        assertThat(getTuples(index, new Pair<>("F", 20))).isEmpty();
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
