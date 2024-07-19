package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class IndexedElementAwareListTest {

    @Test
    void addRemove() {
        IndexedElementAwareList<String> tupleList = new IndexedElementAwareList<>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        String valueA = "A";
        tupleList.add(valueA);

        Assertions.assertThat(tupleList.contains(valueA)).isTrue();

        assertThat(tupleList.size()).isEqualTo(1);

        assertThat(tupleList.next(valueA)).isNull();

        assertThat(tupleList.previous(valueA)).isNull();

        assertThat(tupleList.first()).isEqualTo(valueA);
        assertThat(tupleList.last()).isEqualTo(valueA);

        String valueB = "B";
        tupleList.add(valueB);

        Assertions.assertThat(tupleList.contains(valueA)).isTrue();
        Assertions.assertThat(tupleList.contains(valueB)).isTrue();

        assertThat(tupleList.size()).isEqualTo(2);

        assertThat(tupleList.next(valueA)).isEqualTo(valueB);
        assertThat(tupleList.next(valueB)).isNull();

        assertThat(tupleList.previous(valueA)).isNull();
        assertThat(tupleList.previous(valueB)).isEqualTo(valueA);

        assertThat(tupleList.first()).isEqualTo(valueA);
        assertThat(tupleList.last()).isEqualTo(valueB);

        tupleList.remove(valueA);

        assertThat(tupleList.size()).isEqualTo(1);

        assertThat(tupleList.next(valueB)).isNull();

        assertThat(tupleList.previous(valueB)).isNull();

        assertThat(tupleList.first()).isEqualTo(valueB);
        assertThat(tupleList.last()).isEqualTo(valueB);

        tupleList.remove(valueB);

        assertThat(tupleList.size()).isZero();

        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();
    }

    @Test
    void addFirst() {
        IndexedElementAwareList<String> tupleList = new IndexedElementAwareList<>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        String valueA = "A";
        tupleList.add(valueA);
        String valueB = "B";
        tupleList.add(valueB);
        String valueC = "C";
        tupleList.addFirst(valueC);

        Assertions.assertThat(tupleList.contains(valueA)).isTrue();
        Assertions.assertThat(tupleList.contains(valueB)).isTrue();
        Assertions.assertThat(tupleList.contains(valueC)).isTrue();

        assertThat(tupleList.size()).isEqualTo(3);

        assertThat(tupleList.next(valueC)).isEqualTo(valueA);
        assertThat(tupleList.next(valueA)).isEqualTo(valueB);
        assertThat(tupleList.next(valueB)).isNull();

        assertThat(tupleList.previous(valueC)).isNull();
        assertThat(tupleList.previous(valueA)).isEqualTo(valueC);
        assertThat(tupleList.previous(valueB)).isEqualTo(valueA);

        assertThat(tupleList.first()).isEqualTo(valueC);
        assertThat(tupleList.last()).isEqualTo(valueB);
    }

    @Test
    void addAfter() {
        IndexedElementAwareList<String> tupleList = new IndexedElementAwareList<>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        String valueA = "A";
        tupleList.add(valueA);
        String valueB = "B";
        tupleList.add(valueB);
        String valueC = "C";
        tupleList.addAfter(valueC, valueA);

        Assertions.assertThat(tupleList.contains(valueA)).isTrue();
        Assertions.assertThat(tupleList.contains(valueB)).isTrue();
        Assertions.assertThat(tupleList.contains(valueC)).isTrue();

        assertThat(tupleList.size()).isEqualTo(3);

        assertThat(tupleList.next(valueA)).isEqualTo(valueC);
        assertThat(tupleList.next(valueC)).isEqualTo(valueB);
        assertThat(tupleList.next(valueB)).isNull();

        assertThat(tupleList.previous(valueA)).isNull();
        assertThat(tupleList.previous(valueC)).isEqualTo(valueA);
        assertThat(tupleList.previous(valueB)).isEqualTo(valueC);

        assertThat(tupleList.first()).isEqualTo(valueA);
        assertThat(tupleList.last()).isEqualTo(valueB);

        String valueD = "D";
        tupleList.addAfter(valueD, valueB);

        Assertions.assertThat(tupleList.contains(valueA)).isTrue();
        Assertions.assertThat(tupleList.contains(valueB)).isTrue();
        Assertions.assertThat(tupleList.contains(valueC)).isTrue();
        Assertions.assertThat(tupleList.contains(valueD)).isTrue();

        assertThat(tupleList.size()).isEqualTo(4);

        assertThat(tupleList.next(valueA)).isEqualTo(valueC);
        assertThat(tupleList.next(valueC)).isEqualTo(valueB);
        assertThat(tupleList.next(valueB)).isEqualTo(valueD);
        assertThat(tupleList.next(valueD)).isNull();

        assertThat(tupleList.previous(valueA)).isNull();
        assertThat(tupleList.previous(valueC)).isEqualTo(valueA);
        assertThat(tupleList.previous(valueB)).isEqualTo(valueC);
        assertThat(tupleList.previous(valueD)).isEqualTo(valueB);

        assertThat(tupleList.first()).isEqualTo(valueA);
        assertThat(tupleList.last()).isEqualTo(valueD);

    }

    @Test
    void iterator() {
        // create a list and add some elements
        IndexedElementAwareList<String> list = new IndexedElementAwareList<>();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(list).isEmpty();
            Iterator<String> iter = list.iterator();
            softly.assertThat(iter.hasNext()).isFalse();
            softly.assertThatThrownBy(iter::next).isInstanceOf(NoSuchElementException.class);
        });

        list.add("A");
        list.add("B");
        list.add("C");
        // iterate through the list, ensuring all elements are present
        Iterator<String> iter = list.iterator();
        Assertions.assertThat(iter.hasNext()).isTrue();
        Assertions.assertThat(iter.next()).isEqualTo("A");
        Assertions.assertThat(iter.hasNext()).isTrue();
        Assertions.assertThat(iter.next()).isEqualTo("B");
        Assertions.assertThat(iter.hasNext()).isTrue();
        Assertions.assertThat(iter.next()).isEqualTo("C");
        Assertions.assertThat(iter.hasNext()).isFalse();
    }

}
