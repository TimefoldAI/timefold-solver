package ai.timefold.solver.core.impl.bavet.common.index;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexedSetTest {

    private final ElementPositionTracker<String> stringTracker = new SimpleTracker<>();

    @Test
    void addMultipleElements() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");

        assertThat(set.size()).isEqualTo(3);
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void removeElement() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.remove("A");

        assertThat(set.isEmpty()).isTrue();
        assertThat(set.size()).isZero();
    }

    @Test
    void removeNonExistentElement() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        assertThatThrownBy(() -> set.remove("B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void removeFromEmptySet() {
        var set = new IndexedSet<>(stringTracker);

        assertThatThrownBy(() -> set.remove("A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void removeMiddleElement() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "C");
    }

    @Test
    void removeLastElement() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("C");

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void removeFirstElement() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("A");

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.asList()).containsExactlyInAnyOrder("B", "C");
    }

    @Test
    void multipleRemovalsAndAdditions() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");
        set.add("D");
        set.remove("A");
        set.add("E");

        assertThat(set.size()).isEqualTo(3);
        assertThat(set.asList()).containsExactlyInAnyOrder("C", "D", "E");
    }

    @Test
    void forEach() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");

        var result = new ArrayList<String>();
        set.forEach(result::add);

        assertThat(result).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void forEachOnEmptySet() {
        var set = new IndexedSet<>(stringTracker);

        var result = new ArrayList<String>();
        set.forEach(result::add);

        assertThat(result).isEmpty();
    }

    @Test
    void forEachWithRemoval() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.add("D");

        var result = new ArrayList<String>();
        set.forEach(element -> {
            result.add(element);
            if (element.equals("B")) {
                set.remove("B");
            }
        });

        assertThat(result).containsExactlyInAnyOrder("A", "B", "C", "D");
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "C", "D");
    }

    @Test
    void isEmpty() {
        var set = new IndexedSet<>(stringTracker);

        assertThat(set.isEmpty()).isTrue();

        set.add("A");
        assertThat(set.isEmpty()).isFalse();

        set.remove("A");
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void asListOnEmptySet() {
        var set = new IndexedSet<>(stringTracker);

        assertThat(set.asList()).isEmpty();
    }

    @Test
    void largeSetWithManyRemovals() {
        var intTracker = new SimpleTracker<Integer>();
        var set = new IndexedSet<>(intTracker);

        for (int i = 0; i < 100; i++) {
            set.add(i);
        }

        for (int i = 0; i < 50; i++) {
            set.remove(i * 2);
        }

        assertThat(set.size()).isEqualTo(50);
        for (int i = 0; i < 50; i++) {
            assertThat(set.asList()).contains(i * 2 + 1);
        }
    }

    private static final class SimpleTracker<T> implements ElementPositionTracker<T> {

        private final Map<T, Integer> positions = new HashMap<>();

        @Override
        public void setPosition(T element, int position) {
            positions.put(element, position);
        }

        @Override
        public int clearPosition(T element) {
            var position = positions.remove(element);
            return position == null ? -1 : position;
        }
    }

}
