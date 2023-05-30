package ai.timefold.solver.constraint.streams.bavet.common.collection;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.constraint.streams.bavet.common.tuple.UniTuple;

import org.junit.jupiter.api.Test;

class TupleListTest {

    @Test
    void addRemove() {
        TupleList<UniTuple<String>> tupleList = new TupleList<>();
        assertThat(tupleList.size()).isEqualTo(0);
        assertThat(tupleList.first()).isNull();
        assertThat(tupleList.last()).isNull();

        TupleListEntry<UniTuple<String>> entryA = tupleList.add(new UniTuple<>("A", 0));
        assertThat(entryA.getElement().factA).isEqualTo("A");
        assertThat(tupleList.size()).isEqualTo(1);
        assertThat(tupleList.first()).isEqualTo(entryA);
        assertThat(entryA.previous).isNull();
        assertThat(entryA.next).isNull();
        assertThat(tupleList.last()).isEqualTo(entryA);

        TupleListEntry<UniTuple<String>> entryB = tupleList.add(new UniTuple<>("B", 0));
        assertThat(entryB.getElement().factA).isEqualTo("B");
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

}
