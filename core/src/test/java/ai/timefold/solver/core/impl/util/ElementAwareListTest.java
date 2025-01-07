package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ElementAwareListTest {

    @Test
    void addRemove() {
        var tupleList = new ElementAwareList<String>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        var entryA = tupleList.add("A");
        Assertions.assertThat(entryA.getElement()).isEqualTo("A");
        assertThat(tupleList.size()).isEqualTo(1);
        assertThat(tupleList.first()).isEqualTo(entryA);
        assertThat(entryA.previous).isNull();
        assertThat(entryA.next).isNull();
        assertThat(tupleList.last()).isEqualTo(entryA);

        var entryB = tupleList.add("B");
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
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();
    }

    @Test
    void addFirst() {
        var tupleList = new ElementAwareList<String>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        var entryA = tupleList.add("A");
        var entryB = tupleList.add("B");
        var entryC = tupleList.addFirst("C");

        assertThat(tupleList.size()).isEqualTo(3);

        assertThat(entryC.next).isEqualTo(entryA);
        assertThat(entryA.next).isEqualTo(entryB);
        assertThat(entryB.next).isNull();

        assertThat(entryC.previous).isNull();
        assertThat(entryA.previous).isEqualTo(entryC);
        assertThat(entryB.previous).isEqualTo(entryA);

        assertThat(tupleList.first()).isEqualTo(entryC);
        assertThat(tupleList.last()).isEqualTo(entryB);
    }

    @Test
    void addAfter() {
        var tupleList = new ElementAwareList<String>();
        assertThat(tupleList.size()).isZero();
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        var entryA = tupleList.add("A");
        var entryB = tupleList.add("B");
        var entryC = tupleList.addAfter("C", entryA);

        assertThat(tupleList.size()).isEqualTo(3);

        assertThat(entryA.next).isEqualTo(entryC);
        assertThat(entryC.next).isEqualTo(entryB);
        assertThat(entryB.next).isNull();

        assertThat(entryA.previous).isNull();
        assertThat(entryC.previous).isEqualTo(entryA);
        assertThat(entryB.previous).isEqualTo(entryC);

        assertThat(tupleList.first()).isEqualTo(entryA);
        assertThat(tupleList.last()).isEqualTo(entryB);

        var entryD = tupleList.addAfter("D", entryB);

        assertThat(tupleList.size()).isEqualTo(4);

        assertThat(entryA.next).isEqualTo(entryC);
        assertThat(entryC.next).isEqualTo(entryB);
        assertThat(entryB.next).isEqualTo(entryD);
        assertThat(entryD.next).isNull();

        assertThat(entryA.previous).isNull();
        assertThat(entryC.previous).isEqualTo(entryA);
        assertThat(entryB.previous).isEqualTo(entryC);
        assertThat(entryD.previous).isEqualTo(entryB);

        assertThat(tupleList.first()).isEqualTo(entryA);
        assertThat(tupleList.last()).isEqualTo(entryD);
    }

}
