package ai.timefold.solver.core.impl.bavet.common.index;

import static ai.timefold.solver.core.impl.bavet.common.index.AbstractIndexerTest.toEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Unlike {@link DefaultRetiringRandomIteratorTest}, this iterator needs no cooperation from the
 * caller: it retires every element it returns by itself, so it ends on its own once every element has
 * been drawn exactly once.
 */
class DefaultUniqueRandomIteratorTest {

    @Test
    void drawsEveryElementExactlyOnce() {
        var list = List.of("A", "B", "C", "D", "E");
        var iterator = UniqueRandomIterator.of(toEntries(list), new Random(0));

        var drawnElements = new HashSet<String>();
        for (int i = 0; i < list.size(); i++) {
            assertThat(iterator.hasNext()).isTrue();
            drawnElements.add(iterator.next());
        }

        assertThat(drawnElements).containsExactlyInAnyOrderElementsOf(list);
        assertThat(iterator.hasNext()).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(iterator::next);
    }

    @Test
    void removeThrows() {
        var list = List.of("A");
        var iterator = UniqueRandomIterator.of(toEntries(list), new Random(0));

        iterator.next();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(iterator::remove);
    }

    @Test
    void emptySource() {
        var iterator = UniqueRandomIterator.<String> empty();

        assertThat(iterator.hasNext()).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(iterator::next);
    }

}
