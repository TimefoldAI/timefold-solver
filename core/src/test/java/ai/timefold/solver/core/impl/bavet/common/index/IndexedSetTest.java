package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

class IndexedSetTest {

    private IndexedSet<String> createIndexedSet() {
        return new IndexedSet<>(new CompactingIndexPositionTracker<>());
    }

    @Test
    void addMultipleElements() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.add("C");

        assertThat(set.size()).isEqualTo(3);
        assertThat(set.asList()).containsExactly("A", "B", "C");
    }

    @Test
    void removeLastElement() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.remove("B");

        assertThat(set.size()).isEqualTo(1);
        assertThat(set.asList()).containsExactly("A");
    }

    @Test
    void removeFirstElement() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.remove("A");

        assertThat(set.size()).isEqualTo(1);
        assertThat(set.asList()).containsExactly("B");
    }

    @Test
    void removeMiddleElement() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.asList()).containsExactly("A", "C");
    }

    @Test
    void removeNonExistentElementFails() {
        var set = createIndexedSet();
        set.add("A");

        assertThatThrownBy(() -> set.remove("B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void removeFromEmptySetFails() {
        var set = createIndexedSet();

        assertThatThrownBy(() -> set.remove("A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was not found");
    }

    @Test
    void removeAllElements() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.add("C");

        set.remove("A");
        set.remove("B");
        set.remove("C");

        assertThat(set.size()).isZero();
        assertThat(set.isEmpty()).isTrue();
        assertThat(set.asList()).isEmpty();
    }

    @Test
    void emptySetOperations() {
        var set = createIndexedSet();

        assertThat(set.size()).isZero();
        assertThat(set.isEmpty()).isTrue();
        assertThat(set.asList()).isEmpty();
        assertThat(set.findFirst(e -> true)).isNull();

        var elements = new ArrayList<>();
        set.forEach(elements::add);
        assertThat(elements).isEmpty();
    }

    @Test
    void forEachIteratesAllElements() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.add("C");

        var elements = new ArrayList<String>();
        set.forEach(elements::add);

        assertThat(elements).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void forEachWithGaps() {
        var set = createIndexedSet();
        for (int i = 0; i < 20; i++) {
            set.add("Element-" + i);
        }
        // Remove some elements to create gaps
        set.remove("Element-5");
        set.remove("Element-10");
        set.remove("Element-15");

        var elements = new ArrayList<String>();
        set.forEach(elements::add);

        assertThat(elements)
                .hasSize(17)
                .doesNotContain("Element-5", "Element-10", "Element-15");
    }

    @Test
    void findFirstReturnsFirstMatch() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.add("C");

        var result = set.findFirst(e -> e.equals("B"));

        assertThat(result).isEqualTo("B");
    }

    @Test
    void findFirstReturnsNullWhenNoMatch() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");

        var result = set.findFirst(e -> e.equals("C"));

        assertThat(result).isNull();
    }

    @Test
    void findFirstOnEmptySet() {
        var set = createIndexedSet();

        var result = set.findFirst(e -> true);

        assertThat(result).isNull();
    }

    @Test
    void compactionTriggeredDuringForEach() {
        var set = createIndexedSet();
        // Add enough elements to trigger compaction
        for (int i = 0; i < 20; i++) {
            set.add("Element-" + i);
        }

        // Remove elements to create gaps (more than 10% to trigger compaction)
        set.remove("Element-1");
        set.remove("Element-3");
        set.remove("Element-5");

        var elements = new ArrayList<String>();
        set.forEach(elements::add);

        assertThat(elements).hasSize(17);
        assertThat(set.size()).isEqualTo(17);
    }

    @Test
    void compactionTriggeredDuringAsList() {
        var set = createIndexedSet();
        for (int i = 0; i < 20; i++) {
            set.add("Element-" + i);
        }

        set.remove("Element-1");
        set.remove("Element-3");
        set.remove("Element-5");

        var list = set.asList();

        assertThat(list)
                .hasSize(17)
                .doesNotContainNull()
                .doesNotContain("Element-1", "Element-3", "Element-5");
    }

    @Test
    void multipleExternalRemovalsDuringForEach() {
        var set = createIndexedSet();
        for (int i = 0; i < 30; i++) {
            set.add("Element-" + i);
        }

        var counter = new AtomicInteger(0);
        set.forEach(element -> {
            var count = counter.incrementAndGet();
            if (count == 5) {
                set.remove("Element-15");
            } else if (count == 10) {
                set.remove("Element-20");
            } else if (count == 15) {
                set.remove("Element-25");
            }
        });

        assertThat(set.asList())
                .doesNotContain("Element-25", "Element-20", "Element-15");
    }

    @Test
    void removeAllElementsDuringForEach() {
        var set = createIndexedSet();
        set.add("A");
        set.add("B");
        set.add("C");

        var elements = new ArrayList<String>();
        set.forEach(element -> {
            elements.add(element);
            set.remove(element);
        });

        assertThat(elements).hasSize(3);
        assertThat(set.isEmpty()).isTrue();
        assertThat(set.size()).isZero();
    }

    @Test
    void findFirstWithExternalRemoval() {
        var set = createIndexedSet();
        for (int i = 0; i < 20; i++) {
            set.add("Element-" + i);
        }

        var found = set.findFirst(element -> {
            if (element.equals("Element-5")) {
                set.remove("Element-15");
                return true;
            }
            return false;
        });

        assertThat(found).isEqualTo("Element-5");
        assertThat(set.size()).isEqualTo(19);
    }

    @Test
    void gapAtTheBackIsRemovedDirectly() {
        var set = createIndexedSet();
        for (int i = 0; i < 20; i++) {
            set.add("Element-" + i);
        }

        // Create a gap at position 10
        set.remove("Element-10");

        // During forEach, compaction will move Element-19 to position 10
        // Then create a gap at position 19 (the back)
        set.remove("Element-19");

        var list = set.asList();
        assertThat(list)
                .hasSize(18)
                .doesNotContainNull();
    }

    @Test
    void asListReturnsEmptyListForEmptySet() {
        var set = createIndexedSet();

        assertThat(set.asList()).isEmpty();
    }

    @Test
    void sizeIsCorrectAfterMultipleOperations() {
        var set = createIndexedSet();

        assertThat(set.size()).isZero();

        set.add("A");
        assertThat(set.size()).isEqualTo(1);

        set.add("B");
        assertThat(set.size()).isEqualTo(2);

        set.remove("A");
        assertThat(set.size()).isEqualTo(1);

        set.add("C");
        assertThat(set.size()).isEqualTo(2);

        set.remove("B");
        set.remove("C");
        assertThat(set.size()).isZero();
    }

    @NullMarked
    private static final class CompactingIndexPositionTracker<T> implements ElementPositionTracker<T> {

        private final Map<T, Integer> positionMap = new HashMap<>();

        @Override
        public void setPosition(T element, int position) {
            positionMap.put(element, position);
        }

        @Override
        public int clearPosition(T element) {
            Integer result = positionMap.remove(element);
            return result != null ? result : -1;
        }

    }
}
