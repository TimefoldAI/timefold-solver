package ai.timefold.solver.core.impl.bavet.common.tuple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class TupleListTest {

    private static Tuple createTuple() {
        // Slot 0 = prevStoreIndex, slot 1 = nextStoreIndex for the list under test.
        return UniTuple.of(2);
    }

    @Test
    void addSingle() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();

        list.add(t1);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.first()).isEqualTo(t1);
        assertThat(list.next(t1)).isNull();
    }

    @Test
    void addTwo_firstAndNextCorrect() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();

        list.add(t1);
        list.add(t2);

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.first()).isEqualTo(t1);
        assertThat(list.next(t1)).isEqualTo(t2);
        assertThat(list.next(t2)).isNull();
    }

    @Test
    void removeFirst() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        list.add(t1);
        list.add(t2);

        list.remove(t1);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.first()).isEqualTo(t2);
        assertThat(list.next(t2)).isNull();
    }

    @Test
    void removeLast() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        list.add(t1);
        list.add(t2);

        list.remove(t2);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.first()).isEqualTo(t1);
        assertThat(list.next(t1)).isNull();
    }

    @Test
    void removeMiddle() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        var t3 = createTuple();
        list.add(t1);
        list.add(t2);
        list.add(t3);

        list.remove(t2);

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.first()).isEqualTo(t1);
        assertThat(list.next(t1)).isEqualTo(t3);
        assertThat(list.next(t3)).isNull();
    }

    @Test
    void removeAll_emptyAfter() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        list.add(t1);

        list.remove(t1);

        assertThat(list.size()).isEqualTo(0);
        assertThat(list.first()).isNull();
    }

    @Test
    void clear_callsConsumerInOrder_thenEmpty() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        list.add(t1);
        list.add(t2);

        var visited = new ArrayList<Tuple>();
        list.clear(visited::add);

        assertThat(visited).containsExactly(t1, t2);
        assertThat(list.size()).isEqualTo(0);
        assertThat(list.first()).isNull();
    }

    @Test
    void clear_safeWhenConsumerRemovesFromSameList() {
        // Verifies that pre-capturing 'next' in clear() makes it safe
        // even if the consumer calls remove() on the same list;
        // the walk is not corrupted.
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        var t3 = createTuple();
        list.add(t1);
        list.add(t2);
        list.add(t3);

        var visited = new ArrayList<Tuple>();
        list.clear(tuple -> {
            visited.add(tuple);
            // Remove from the same list while clear() is walking;
            // safe because clear() pre-captures next.
            // In production, retractOutTupleByLeft removes from the *other* side's list, not this one,
            // so this scenario is stronger than production requires.
            if (tuple == t2) {
                list.remove(tuple);
            }
        });

        // All three elements are visited regardless of the mid-walk removal.
        assertThat(visited).containsExactly(t1, t2, t3);
        assertThat(list.size()).isEqualTo(0);
        assertThat(list.first()).isNull();
    }

    @Test
    void forEach_visitAllInOrder() {
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        var t3 = createTuple();
        list.add(t1);
        list.add(t2);
        list.add(t3);

        var visited = new ArrayList<Tuple>();
        list.forEach(visited::add);

        assertThat(visited).containsExactly(t1, t2, t3);
    }

    @Test
    void emptyList_sizeZero_firstNull() {
        var list = new TupleList<>(0, 1);

        assertThat(list.size()).isEqualTo(0);
        assertThat(list.first()).isNull();
    }

    @Test
    void remove_nullsSlotsOnTuple() {
        // Verify that remove clears both link slots so a removed tuple
        // doesn't retain stale pointers (important for GC and correctness).
        var list = new TupleList<>(0, 1);
        var t1 = createTuple();
        var t2 = createTuple();
        list.add(t1);
        list.add(t2);

        list.remove(t1);

        // After removal, the tuple's slots must be null.
        assertThat(t1.<Object> getStore(0)).isNull(); // prevSlot
        assertThat(t1.<Object> getStore(1)).isNull(); // nextSlot
    }
}
