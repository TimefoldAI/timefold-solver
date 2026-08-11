package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

/**
 * {@link Indexer#randomIterator(Object, RandomGenerator)} and
 * {@link Indexer#uniqueRandomIterator(Object, RandomGenerator)} return the corresponding marker type
 * regardless of which {@link Indexer} implementation is asked, and {@code remove()}/
 * {@code forEachRemaining()} behave accordingly.
 */
class IndexerRandomIteratorMarkerTest {

    @Test
    void leafIndexerMarkerTypes() {
        Indexer<UniTuple<String>> indexer = new RandomAccessLeafIndexer<>();
        indexer.put(CompositeKey.none(), UniTuple.of("Ann", 0));
        assertMarkerTypes(indexer, CompositeKey.none());
    }

    @Test
    void equalIndexerMarkerTypes() {
        Indexer<UniTuple<String>> indexer = new EqualIndexer<>(KeyUnpacker.single(), RandomAccessLeafIndexer::new);
        indexer.put("F", UniTuple.of("Ann", 0));
        assertMarkerTypes(indexer, "F");
    }

    private static void assertMarkerTypes(Indexer<UniTuple<String>> indexer, Object key) {
        var repeating = indexer.randomIterator(key, new Random(0));
        var unique = indexer.uniqueRandomIterator(key, new Random(0));

        assertThat(repeating).isInstanceOf(RepeatingRandomIterator.class);
        assertThat(unique).isInstanceOf(UniqueRandomIterator.class);

        assertThatThrownBy(repeating::remove).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(unique::remove).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> repeating.forEachRemaining(t -> {
        })).isInstanceOf(UnsupportedOperationException.class);
    }

}
