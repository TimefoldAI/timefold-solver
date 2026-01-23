package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class RandomAccessIndexerBackendTest extends AbstractIndexerTest {

    @Test
    void isRemovable() {
        var indexer = new RandomAccessIndexerBackend<>();
        assertSoftly(softly -> {
            softly.assertThat(forEachToTuples(indexer)).isEmpty();
            softly.assertThat(indexer.isRemovable()).isTrue();
        });
    }

    @Test
    void put() {
        var indexer = new RandomAccessIndexerBackend<>();
        var annTuple = newTuple("Ann-F-40");
        assertThat(indexer.size(CompositeKey.none())).isZero();
        indexer.put(CompositeKey.none(), annTuple);
        assertThat(indexer.size(CompositeKey.none())).isEqualTo(1);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isRemovable()).isFalse();
            softly.assertThat(forEachToTuples(indexer)).containsExactly(annTuple);
        });
    }

    @Test
    void removeTwice() {
        var indexer = new RandomAccessIndexerBackend<>();
        var annTuple = newTuple("Ann-F-40");
        var annEntry = indexer.put(CompositeKey.none(), annTuple);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isRemovable()).isFalse();
            softly.assertThat(forEachToTuples(indexer)).containsExactly(annTuple);
        });

        indexer.remove(CompositeKey.none(), annEntry);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isRemovable()).isTrue();
            softly.assertThat(forEachToTuples(indexer)).isEmpty();
        });
        assertThatThrownBy(() -> indexer.remove(CompositeKey.none(), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void forEach() {
        var indexer = new RandomAccessIndexerBackend<>();

        var annTuple = newTuple("Ann-F-40");
        indexer.put(CompositeKey.none(), annTuple);
        var bethTuple = newTuple("Beth-F-30");
        indexer.put(CompositeKey.none(), bethTuple);

        assertThat(forEachToTuples(indexer)).containsOnly(annTuple, bethTuple);
    }

    private static UniTuple<String> newTuple(String factA) {
        return UniTuple.of(factA, 0);
    }

}
