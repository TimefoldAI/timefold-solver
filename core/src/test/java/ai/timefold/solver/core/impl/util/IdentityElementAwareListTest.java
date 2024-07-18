package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class IdentityElementAwareListTest {

    @Test
    void addRemove() {
        IdentityElementAwareList<String> tupleList = new IdentityElementAwareList<>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        String valueA = "A";
        tupleList.add(valueA);
        Assertions.assertThat(tupleList.contains(valueA)).isTrue();
        assertThat(tupleList.size()).isEqualTo(1);
        assertThat(tupleList.first()).isEqualTo(valueA);
        assertThat(tupleList.previous(valueA)).isNull();
        assertThat(tupleList.next(valueA)).isNull();
        assertThat(tupleList.last()).isEqualTo(valueA);

        String valueB = "B";
        tupleList.add(valueB);
        Assertions.assertThat(tupleList.contains(valueB)).isTrue();
        assertThat(tupleList.size()).isEqualTo(2);
        assertThat(tupleList.first()).isEqualTo(valueA);
        assertThat(tupleList.previous(valueA)).isNull();
        assertThat(tupleList.next(valueA)).isEqualTo(valueB);
        assertThat(tupleList.previous(valueB)).isEqualTo(valueA);
        assertThat(tupleList.next(valueB)).isNull();
        assertThat(tupleList.last()).isEqualTo(valueB);

        tupleList.remove(valueA);
        assertThat(tupleList.size()).isEqualTo(1);
        assertThat(tupleList.first()).isEqualTo(valueB);
        assertThat(tupleList.previous(valueB)).isNull();
        assertThat(tupleList.next(valueB)).isNull();
        assertThat(tupleList.last()).isEqualTo(valueB);

        tupleList.remove(valueB);
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();
    }

    @Test
    void iterator() {
        // create a list and add some elements
        IdentityElementAwareList<String> list = new IdentityElementAwareList<>();
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
