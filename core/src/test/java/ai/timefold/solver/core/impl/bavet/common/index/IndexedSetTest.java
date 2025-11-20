package ai.timefold.solver.core.impl.bavet.common.index;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@NullMarked
class IndexedSetTest {

    @Test
    void addSingleElement() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");

        assertThat(set.size()).isEqualTo(1);
        assertThat(set.isEmpty()).isFalse();
        assertThat(tracker.hasPosition("A")).isTrue();
    }

    @Test
    void addMultipleElements() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.add("C");

        assertThat(set.size()).isEqualTo(3);
        assertThat(set.isEmpty()).isFalse();
        assertThat(tracker.hasPosition("A")).isTrue();
        assertThat(tracker.hasPosition("B")).isTrue();
        assertThat(tracker.hasPosition("C")).isTrue();
    }

    @Test
    void removeLastElement() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.remove("B");

        assertThat(set.size()).isEqualTo(1);
        assertThat(tracker.hasPosition("A")).isTrue();
        assertThat(tracker.hasPosition("B")).isFalse();
    }

    @Test
    void removeMiddleElement() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");

        assertThat(set.size()).isEqualTo(2);
        assertThat(tracker.hasPosition("A")).isTrue();
        assertThat(tracker.hasPosition("B")).isFalse();
        assertThat(tracker.hasPosition("C")).isTrue();
    }

    @Test
    void removeNonExistentElement() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");

        assertThatThrownBy(() -> set.remove("B"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was not found in the IndexedSet");
    }

    @Test
    void removeAllElements() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.remove("A");
        set.remove("B");

        assertThat(set.size()).isEqualTo(0);
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void removeAllElementsCreatingGaps() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");
        set.remove("A");
        set.remove("C");

        assertThat(set.size()).isEqualTo(0);
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void forEachEmptySet() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        set.forEach(result::add);

        assertThat(result).isEmpty();
    }

    @Test
    void forEachWithoutGaps() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        set.add("A");
        set.add("B");
        set.add("C");
        set.forEach(result::add);

        assertThat(result).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void forEachWithGapsNoCompaction() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        // Add fewer elements than MINIMUM_ELEMENT_COUNT_FOR_COMPACTION
        for (int i = 0; i < IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION - 1; i++) {
            set.add("Element" + i);
        }
        set.remove("Element5");
        set.forEach(result::add);

        assertThat(result).hasSize(IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION - 2);
    }

    @Test
    void forEachWithGapsTriggersCompaction() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        // Add enough elements to trigger compaction
        int elementCount = IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION;
        for (int i = 0; i < elementCount; i++) {
            set.add("Element" + i);
        }

        // Remove enough elements to exceed GAP_RATIO_FOR_COMPACTION
        int gapsNeeded = (int) Math.ceil(elementCount * IndexedSet.GAP_RATIO_FOR_COMPACTION) + 1;
        for (int i = 0; i < gapsNeeded; i++) {
            set.remove("Element" + i);
        }

        set.forEach(result::add);

        assertThat(result).hasSize(elementCount - gapsNeeded);
    }

    @Test
    void findFirstEmptySet() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        var result = set.findFirst(s -> s.equals("A"));

        assertThat(result).isNull();
    }

    @Test
    void findFirstWithoutGaps() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.add("C");

        var result = set.findFirst(s -> s.equals("B"));

        assertThat(result).isEqualTo("B");
    }

    @Test
    void findFirstNotFound() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");

        var result = set.findFirst(s -> s.equals("C"));

        assertThat(result).isNull();
    }

    @Test
    void findFirstWithGapsNoCompaction() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        for (int i = 0; i < IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION - 1; i++) {
            set.add("Element" + i);
        }
        set.remove("Element5");

        var result = set.findFirst(s -> s.equals("Element10"));

        assertThat(result).isEqualTo("Element10");
    }

    @Test
    void findFirstWithGapsTriggersCompaction() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        int elementCount = IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION;
        for (int i = 0; i < elementCount; i++) {
            set.add("Element" + i);
        }

        int gapsNeeded = (int) Math.ceil(elementCount * IndexedSet.GAP_RATIO_FOR_COMPACTION) + 1;
        for (int i = 0; i < gapsNeeded; i++) {
            set.remove("Element" + i);
        }

        var result = set.findFirst(s -> s.startsWith("Element"));

        assertThat(result).isNotNull();
    }

    @Test
    void clearEmptySet() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        set.clear(result::add);

        assertThat(result).isEmpty();
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void clearWithElements() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        set.add("A");
        set.add("B");
        set.add("C");
        set.clear(result::add);

        assertThat(result).containsExactlyInAnyOrder("A", "B", "C");
        assertThat(set.isEmpty()).isTrue();
        assertThat(tracker.hasPosition("A")).isFalse();
        assertThat(tracker.hasPosition("B")).isFalse();
        assertThat(tracker.hasPosition("C")).isFalse();
    }

    @Test
    void clearWithGaps() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);
        var result = new ArrayList<String>();

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");
        set.clear(result::add);

        assertThat(result).containsExactlyInAnyOrder("A", "C");
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void asListEmptySet() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        var list = set.asList();

        assertThat(list).isEmpty();
    }

    @Test
    void asListWithoutGaps() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.add("C");

        var list = set.asList();

        assertThat(list).containsExactly("A", "B", "C");
    }

    @Test
    void asListWithGapsForcesCompaction() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");

        var list = set.asList();

        assertThat(list)
                .hasSize(2)
                .doesNotContainNull()
                .containsExactlyInAnyOrder("A", "C");
    }

    @Test
    void asListAfterClearReturnsEmptyList() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        set.add("A");
        set.add("B");
        set.remove("A");
        set.remove("B");

        var list = set.asList();

        assertThat(list).isEmpty();
    }

    @Test
    void compactionFillsGapsCorrectly() {
        var tracker = new TestElementPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        // Create scenario where compaction will move elements
        int elementCount = IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION;
        for (int i = 0; i < elementCount; i++) {
            set.add("Element" + i);
        }

        // Remove elements from the beginning to create gaps
        int gapsNeeded = (int) Math.ceil(elementCount * IndexedSet.GAP_RATIO_FOR_COMPACTION) + 1;
        for (int i = 0; i < gapsNeeded; i++) {
            set.remove("Element" + i);
        }

        // Force compaction via asList
        var list = set.asList();

        assertThat(list)
                .hasSize(elementCount - gapsNeeded)
                .doesNotContainNull();
    }

    private static final class TestElementPositionTracker<T> implements ElementPositionTracker<T> {

        private final Map<T, Integer> positionMap = new HashMap<>();

        @Override
        public void setPosition(T element, int position) {
            positionMap.put(element, position);
        }

        @Override
        public int clearPosition(T element) {
            var position = positionMap.remove(element);
            return position != null ? position : -1;
        }

        public boolean hasPosition(T element) {
            return positionMap.containsKey(element);
        }
    }

}
