package ai.timefold.solver.core.impl.bavet.common.index;

import static ai.timefold.solver.core.impl.bavet.common.index.SelectionProbabilityTest.toEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.SoftAssertions.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.junit.jupiter.api.Test;

class DefaultRepeatingRandomIteratorTest {

    @Test
    void emptySource() {
        var empty = new DefaultRepeatingRandomIterator<>(new ElementAwareArrayList<>(), new Random(0));

        assertSoftly(softly -> {
            softly.assertThat(empty.hasNext()).isFalse();
            softly.assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(empty::next);
            softly.assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(empty::remove);
        });
    }

    @Test
    void removeIsNotSupported() {
        var iterator = new DefaultRepeatingRandomIterator<>(toEntries(List.of("A")), new Random(0));

        iterator.next();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(iterator::remove);
    }

    @Test
    void forEachRemainingIsNotSupported() {
        var iterator = new DefaultRepeatingRandomIterator<>(toEntries(List.of("A")), new Random(0));

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> iterator.forEachRemaining(e -> {
        }));
    }

    @Test
    void neverEnds() {
        var list = List.of("A", "B", "C");
        var iterator = new DefaultRepeatingRandomIterator<>(toEntries(list), new Random(0));

        // Draw far more times than there are elements; it must still never run dry.
        for (var i = 0; i < list.size() * 100; i++) {
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.next()).isIn(list);
        }
        assertThat(iterator.hasNext()).isTrue();
    }

    @Test
    void singleElementRepeats() {
        var iterator = new DefaultRepeatingRandomIterator<>(toEntries(List.of("A")), new Random(0));

        for (var i = 0; i < 10; i++) {
            assertThat(iterator.hasNext()).isTrue();
            assertThat(iterator.next()).isEqualTo("A");
        }
    }

}
