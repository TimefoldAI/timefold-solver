package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class ElementAwareListTest {

    @Test
    void addRemove() {
        ElementAwareList<String> tupleList = new ElementAwareList<>();
        assertThat(tupleList.size()).isEqualTo(0);
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        ElementAwareListEntry<String> entryA = tupleList.add("A");
        Assertions.assertThat(entryA.getElement()).isEqualTo("A");
        assertThat(tupleList.size()).isEqualTo(1);
        assertThat(tupleList.first()).isEqualTo(entryA);
        assertThat(entryA.previous).isNull();
        assertThat(entryA.next).isNull();
        assertThat(tupleList.last()).isEqualTo(entryA);

        ElementAwareListEntry<String> entryB = tupleList.add("B");
        Assertions.assertThat(entryB.getElement()).isEqualTo("B");
        assertThat(tupleList.size()).isEqualTo(2);
        assertThat(tupleList.first()).isEqualTo(entryA);
        assertThat(entryA.previous).isNull();
        assertThat(entryA.next).isEqualTo(entryB);
        assertThat(entryB.previous).isEqualTo(entryA);
        assertThat(entryB.next).isNull();
        assertThat(tupleList.last()).isEqualTo(entryB);

        entryA.remove();
        assertThat(tupleList.size()).isEqualTo(1);
        assertThat(tupleList.first()).isEqualTo(entryB);
        assertThat(entryB.previous).isNull();
        assertThat(entryB.next).isNull();
        assertThat(tupleList.last()).isEqualTo(entryB);

        entryB.remove();
        assertThat(tupleList.size()).isEqualTo(0);
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();
    }

    @Test
    public void iterator() {
        // create a list and add some elements
        ElementAwareList<String> list = new ElementAwareList<>();
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
