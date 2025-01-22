package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class NoneIndexerTest extends AbstractIndexerTest {

    @Test
    void isEmpty() {
        var indexer = new NoneIndexer<>();
        assertSoftly(softly -> {
            softly.assertThat(getTuples(indexer)).isEmpty();
            softly.assertThat(indexer.isEmpty()).isTrue();
        });
    }

    @Test
    void put() {
        var indexer = new NoneIndexer<>();
        var annTuple = newTuple("Ann-F-40");
        assertThat(indexer.size(IndexKeys.none())).isEqualTo(0);
        indexer.put(IndexKeys.none(), annTuple);
        assertThat(indexer.size(IndexKeys.none())).isEqualTo(1);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isEmpty()).isFalse();
            softly.assertThat(getTuples(indexer)).containsExactly(annTuple);
        });
    }

    @Test
    void removeTwice() {
        var indexer = new NoneIndexer<>();
        var annTuple = newTuple("Ann-F-40");
        var annEntry = indexer.put(IndexKeys.none(), annTuple);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isEmpty()).isFalse();
            softly.assertThat(getTuples(indexer)).containsExactly(annTuple);
        });

        indexer.remove(IndexKeys.none(), annEntry);
        assertSoftly(softly -> {
            softly.assertThat(indexer.isEmpty()).isTrue();
            softly.assertThat(getTuples(indexer)).isEmpty();
        });
        assertThatThrownBy(() -> indexer.remove(IndexKeys.none(), annEntry))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void visit() {
        var indexer = new NoneIndexer<>();

        var annTuple = newTuple("Ann-F-40");
        indexer.put(IndexKeys.none(), annTuple);
        var bethTuple = newTuple("Beth-F-30");
        indexer.put(IndexKeys.none(), bethTuple);

        assertThat(getTuples(indexer)).containsOnly(annTuple, bethTuple);
    }

    private static UniTuple<String> newTuple(String factA) {
        return new UniTuple<>(factA, 0);
    }

}
