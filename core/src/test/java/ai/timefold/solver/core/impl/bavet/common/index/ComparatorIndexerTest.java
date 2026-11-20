package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.common.joiner.ComparatorKey;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class ComparatorIndexerTest extends AbstractIndexerTest {

    private record Label(String text) {
    }

    private final Comparator<Label> byLength = Comparator.comparingInt(label -> label.text().length());
    private final DefaultBiJoiner<Label, Label> lessThanByLength =
            (DefaultBiJoiner<Label, Label>) Joiners.lessThan(Function.identity(), byLength);

    private ComparatorKey<Label> key(String text) {
        return new ComparatorKey<>(new Label(text), byLength);
    }

    @Test
    void leftBridgeUsesComparatorOrder() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanByLength).buildIndexer(true);
        var shortTuple = newTuple("a");
        var mediumTuple = newTuple("bb");
        var longTuple = newTuple("aaa");
        indexer.put(key("a"), shortTuple);
        indexer.put(key("bb"), mediumTuple);
        indexer.put(key("aaa"), longTuple);

        assertThat(forEachToTuples(indexer, key("bb"))).containsExactly(shortTuple);
        assertThat(forEachToTuples(indexer, key("aaa"))).containsExactlyInAnyOrder(shortTuple, mediumTuple);
        assertThat(forEachToTuples(indexer, key("a"))).isEmpty();
        assertThat(indexer.size(key("aaa"))).isEqualTo(2);
    }

    @Test
    void rightBridgeFlipsComparatorOrder() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanByLength).buildIndexer(false);
        var shortTuple = newTuple("a");
        var mediumTuple = newTuple("bb");
        indexer.put(key("a"), shortTuple);
        indexer.put(key("bb"), mediumTuple);

        assertThat(forEachToTuples(indexer, key("a"))).containsExactly(mediumTuple);
        assertThat(forEachToTuples(indexer, key("bb"))).isEmpty();
    }

    @Test
    void comparatorEqualityDrivesInclusiveJoiner() {
        var joiner = (DefaultBiJoiner<String, String>) Joiners.lessThanOrEqual(Function.identity(), Function.identity(),
                String.CASE_INSENSITIVE_ORDER);
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(joiner).buildIndexer(true);
        var tuple = newTuple("a");
        indexer.put(new ComparatorKey<>("a", String.CASE_INSENSITIVE_ORDER), tuple);

        assertThat(forEachToTuples(indexer, new ComparatorKey<>("A", String.CASE_INSENSITIVE_ORDER)))
                .containsExactly(tuple);
    }

    @Test
    void treeModeUsesComparatorOrder() {
        Indexer<UniTuple<String>> indexer = new IndexerFactory<>(lessThanByLength).buildIndexer(true);
        for (var i = 0; i < 100; i++) {
            indexer.put(key("x".repeat(i + 1)), newTuple("length" + i));
        }

        assertThat(indexer.size(key("x".repeat(100)))).isEqualTo(99);
        assertThat(indexer.size(key("x".repeat(1)))).isZero();
    }

    private static UniTuple<String> newTuple(String factA) {
        return UniTuple.of(factA, 0);
    }
}
