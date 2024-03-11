package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.score.stream.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.score.stream.common.bi.DefaultBiJoiner;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;
import ai.timefold.solver.core.impl.util.Pair;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class EqualsIndexerTest extends AbstractIndexerTest {

    private final DefaultBiJoiner<Person, Person> joiner =
            (DefaultBiJoiner<Person, Person>) Joiners.equal((Person p) -> p.gender)
                    .and(Joiners.equal((Person p) -> p.age));

    @Test
    void isEmpty() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory(joiner).buildIndexer(true);
        Assertions.assertThat(getTuples(indexer, "F", 40)).isEmpty();
    }

    @Test
    void put() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory(joiner).buildIndexer(true);
        UniTuple<String> annTuple = newTuple("Ann-F-40");
        assertThat(indexer.size(new ManyIndexProperties("F", 40))).isEqualTo(0);
        indexer.put(new ManyIndexProperties("F", 40), annTuple);
        assertThat(indexer.size(new ManyIndexProperties("F", 40))).isEqualTo(1);
    }

    @Test
    void removeTwice() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory(joiner).buildIndexer(true);
        UniTuple<String> annTuple = newTuple("Ann-F-40");
        ElementAwareListEntry<UniTuple<String>> annEntry = indexer.put(new ManyIndexProperties("F", 40), annTuple);

        indexer.remove(new ManyIndexProperties("F", 40), annEntry);
        assertThatThrownBy(() -> indexer.remove(new ManyIndexProperties("F", 40), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory(joiner).buildIndexer(true);

        UniTuple<String> annTuple = newTuple("Ann-F-40");
        indexer.put(new SingleIndexProperties(new Pair<>("F", 40)), annTuple);
        UniTuple<String> bethTuple = newTuple("Beth-F-30");
        indexer.put(new SingleIndexProperties(new Pair<>("F", 30)), bethTuple);
        indexer.put(new SingleIndexProperties(new Pair<>("M", 40)), newTuple("Carl-M-40"));
        indexer.put(new SingleIndexProperties(new Pair<>("M", 30)), newTuple("Dan-M-30"));
        UniTuple<String> ednaTuple = newTuple("Edna-F-40");
        indexer.put(new SingleIndexProperties(new Pair<>("F", 40)), ednaTuple);

        Assertions.assertThat(getTuples(indexer, new Pair<>("F", 40))).containsOnly(annTuple, ednaTuple);
        Assertions.assertThat(getTuples(indexer, new Pair<>("F", 30))).containsOnly(bethTuple);
        Assertions.assertThat(getTuples(indexer, new Pair<>("F", 20))).isEmpty();
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
