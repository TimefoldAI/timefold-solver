package ai.timefold.solver.core.impl.bavet.common.index;

import static ai.timefold.solver.core.impl.bavet.common.index.SelectionProbabilityTest.toEntries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class DefaultUniqueRandomIteratorTest {

    @Test
    void emptySet() {
        var emptySet = new DefaultUniqueRandomIterator<>(new ElementAwareArrayList<>(), new Random(0));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(emptySet.hasNext())
                    .isFalse();
            softly.assertThatExceptionOfType(NoSuchElementException.class)
                    .isThrownBy(emptySet::next);
            softly.assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(emptySet::remove);
        });
    }

    @Test
    void singleElementSetPickAndRemove() {
        var list = List.of("A");
        var set = new DefaultUniqueRandomIterator<>(toEntries(list), new Random(0));

        assertThat(set.hasNext()).isTrue();

        var element = set.next();
        assertThat(element).isEqualTo("A");

        set.remove();
        assertThat(set.hasNext()).isFalse();

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(set::next);
    }

    @Test
    void multipleElementSet() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomIterator<>(toEntries(list), new Random(0));

        assertThat(set.hasNext()).isTrue();

        var element = set.next();
        assertThat(element).isIn(list);
    }

    @Test
    void pickDoesNotModifySet() {
        var list = List.of("A", "B", "C");
        var set = new DefaultUniqueRandomIterator<>(toEntries(list), new Random(0));

        var element1 = set.next();
        var element2 = set.next();
        var element3 = set.next();

        assertThat(set.hasNext()).isTrue();
        // Elements may be the same since pick doesn't clear
        assertThat(element1).isIn(list);
        assertThat(element2).isIn(list);
        assertThat(element3).isIn(list);
    }

    @Test
    void removeAllElements() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomIterator<>(toEntries(list), new Random(0));

        var clearedElements = new HashSet<String>();
        for (int i = 0; i < 5; i++) {
            assertThat(set.hasNext()).isTrue();
            clearedElements.add(set.next());
            set.remove();
        }

        assertThat(set.hasNext()).isFalse();
        assertThat(clearedElements).containsExactlyInAnyOrderElementsOf(list);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(set::next);
    }

}
