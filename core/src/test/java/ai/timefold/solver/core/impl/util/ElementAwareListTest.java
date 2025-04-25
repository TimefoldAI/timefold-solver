package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.testutil.TestRandom;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ElementAwareListTest {

    @Test
    void addRemove() {
        var list = new ElementAwareList<String>();
        assertThat(list.size()).isZero();
        assertThat(list.first()).isNull();
        assertThat(list.last()).isNull();

        var entryA = list.add("A");
        assertThat(entryA.getElement()).isEqualTo("A");
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.first()).isEqualTo(entryA);
        assertThat(entryA.previous).isNull();
        assertThat(entryA.next).isNull();
        assertThat(list.last()).isEqualTo(entryA);

        var entryB = list.add("B");
        assertThat(entryB.getElement()).isEqualTo("B");
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.first()).isEqualTo(entryA);
        assertThat(entryA.previous).isNull();
        assertThat(entryA.next).isEqualTo(entryB);
        assertThat(entryB.previous).isEqualTo(entryA);
        assertThat(entryB.next).isNull();
        assertThat(list.last()).isEqualTo(entryB);

        entryA.remove();
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.first()).isEqualTo(entryB);
        assertThat(entryB.previous).isNull();
        assertThat(entryB.next).isNull();
        assertThat(list.last()).isEqualTo(entryB);

        entryB.remove();
        assertThat(list.size()).isZero();
        assertThat(list.first()).isNull();
        assertThat(list.last()).isNull();
    }

    @Test
    void addFirst() {
        var list = new ElementAwareList<String>();
        assertThat(list.size()).isZero();
        assertThat(list.first()).isNull();
        assertThat(list.last()).isNull();

        var entryA = list.add("A");
        var entryB = list.add("B");
        var entryC = list.addFirst("C");

        assertThat(list.size()).isEqualTo(3);

        assertThat(entryC.next).isEqualTo(entryA);
        assertThat(entryA.next).isEqualTo(entryB);
        assertThat(entryB.next).isNull();

        assertThat(entryC.previous).isNull();
        assertThat(entryA.previous).isEqualTo(entryC);
        assertThat(entryB.previous).isEqualTo(entryA);

        assertThat(list.first()).isEqualTo(entryC);
        assertThat(list.last()).isEqualTo(entryB);
    }

    @Test
    void addAfter() {
        var list = new ElementAwareList<String>();
        assertThat(list.size()).isZero();
        assertThat(list.first()).isNull();
        assertThat(list.last()).isNull();

        var entryA = list.add("A");
        var entryB = list.add("B");
        var entryC = list.addAfter("C", entryA);

        assertThat(list.size()).isEqualTo(3);

        assertThat(entryA.next).isEqualTo(entryC);
        assertThat(entryC.next).isEqualTo(entryB);
        assertThat(entryB.next).isNull();

        assertThat(entryA.previous).isNull();
        assertThat(entryC.previous).isEqualTo(entryA);
        assertThat(entryB.previous).isEqualTo(entryC);

        assertThat(list.first()).isEqualTo(entryA);
        assertThat(list.last()).isEqualTo(entryB);

        var entryD = list.addAfter("D", entryB);

        assertThat(list.size()).isEqualTo(4);

        assertThat(entryA.next).isEqualTo(entryC);
        assertThat(entryC.next).isEqualTo(entryB);
        assertThat(entryB.next).isEqualTo(entryD);
        assertThat(entryD.next).isNull();

        assertThat(entryA.previous).isNull();
        assertThat(entryC.previous).isEqualTo(entryA);
        assertThat(entryB.previous).isEqualTo(entryC);
        assertThat(entryD.previous).isEqualTo(entryB);

        assertThat(list.first()).isEqualTo(entryA);
        assertThat(list.last()).isEqualTo(entryD);
    }

    @Test
    void iterator() {
        // create a list and add some elements
        var list = new ElementAwareList<String>();
        assertSoftly(softly -> {
            softly.assertThat(list).isEmpty();
            var iter = list.iterator();
            softly.assertThat(iter.hasNext()).isFalse();
            softly.assertThatThrownBy(iter::next).isInstanceOf(NoSuchElementException.class);
        });

        list.add("A");
        list.add("B");
        list.add("C");
        // iterate through the list, ensuring all elements are present
        var iter = list.iterator();
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo("A");
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo("B");
        assertThat(iter.hasNext()).isTrue();
        assertThat(iter.next()).isEqualTo("C");
        assertThat(iter.hasNext()).isFalse();
    }

    @Test
    void randomizedIterator() {
        // create a list and add some elements
        var list = new ElementAwareList<String>();
        assertSoftly(softly -> {
            var iter = list.randomizedIterator(new Random(0));
            softly.assertThat(iter.hasNext()).isFalse();
            softly.assertThatThrownBy(iter::next).isInstanceOf(NoSuchElementException.class);
        });

        list.add("A");
        assertOrder(list, new String[] { "A" }, 0);

        // Each order of elements should be generated exactly once, to guarantee fair shuffling.
        // The particular order doesn't matter, as long as each combination is listed once.
        var bEntry = list.add("B");
        assertOrder(list, new String[] { "B", "A" }, 0, 0);
        assertOrder(list, new String[] { "A", "B" }, 1, 0);

        list.add("C");
        assertOrder(list, new String[] { "A", "B", "C" }, 0, 0, 0);
        assertOrder(list, new String[] { "A", "C", "B" }, 0, 1, 0);
        assertOrder(list, new String[] { "B", "A", "C" }, 1, 0, 0);
        assertOrder(list, new String[] { "B", "C", "A" }, 1, 1, 0);
        assertOrder(list, new String[] { "C", "A", "B" }, 2, 0, 0);
        assertOrder(list, new String[] { "C", "B", "A" }, 2, 1, 0);

        bEntry.remove();
        assertOrder(list, new String[] { "C", "A" }, 0, 0);
        assertOrder(list, new String[] { "A", "C" }, 1, 0);
    }

    @Test
    void clear() {
        var list = new ElementAwareList<String>();
        list.add("A");
        list.add("B");
        assertThat(list.size()).isEqualTo(2);
        list.clear();
        assertThat(list.size()).isZero();
    }

    private void assertOrder(ElementAwareList<String> list, String[] elements, int... randoms) {
        var iter = list.randomizedIterator(new TestRandom(randoms));
        Assertions.assertThat(iter)
                .toIterable()
                .containsExactly(elements);
    }

}
