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

    @Test
    void compactionTriggersAtCorrectThreshold() {
        var set = createIndexedSet();
        // Add 100 elements (minimum for compaction)
        for (int i = 0; i < 100; i++) {
            set.add("Element-" + i);
        }

        // Remove 10 elements to create exactly 10% gaps (threshold)
        for (int i = 0; i < 10; i++) {
            set.remove("Element-" + i);
        }

        // One more removal should trigger compaction during forEach
        set.remove("Element-10");

        var elements = new ArrayList<String>();
        set.forEach(elements::add);

        assertThat(elements).hasSize(89);
        assertThat(set.asList()).hasSize(89).doesNotContainNull();
    }

    @Test
    void compactionDoesNotTriggerBelowMinimumElements() {
        var set = createIndexedSet();
        // Add 99 elements (below minimum for compaction)
        for (int i = 0; i < 99; i++) {
            set.add("Element-" + i);
        }

        // Remove 50% of elements (well above threshold)
        for (int i = 0; i < 50; i++) {
            set.remove("Element-" + i);
        }

        var elements = new ArrayList<String>();
        set.forEach(elements::add);

        assertThat(elements).hasSize(49);
        // Even though gaps exist, compaction shouldn't have happened during forEach
        // It should happen during asList() instead
        assertThat(set.asList()).hasSize(49).doesNotContainNull();
    }

    @Test
    void compactionDoesNotTriggerBelowGapThreshold() {
        var set = createIndexedSet();
        // Add 100 elements
        for (int i = 0; i < 100; i++) {
            set.add("Element-" + i);
        }

        // Remove 9 elements (9% gaps, below 10% threshold)
        for (int i = 0; i < 9; i++) {
            set.remove("Element-" + i);
        }

        var elements = new ArrayList<String>();
        set.forEach(elements::add);

        assertThat(elements).hasSize(91);
    }

    @Test
    void externalRemovalDuringCompactionAtStart() {
        var set = createIndexedSet();
        for (int i = 0; i < 150; i++) {
            set.add("Element-" + i);
        }

        // Create gaps to trigger compaction (20 gaps = 13.3%)
        for (int i = 0; i < 20; i++) {
            set.remove("Element-" + i);
        }

        var processedElements = new ArrayList<String>();
        var counter = new AtomicInteger(0);

        set.forEach(element -> {
            processedElements.add(element);
            if (counter.incrementAndGet() == 1) {
                // Remove element during compaction (compaction happens back-to-front)
                set.remove("Element-100");
            }
        });

        assertThat(processedElements).hasSize(129); // 150 - 20 - 1
        assertThat(set.asList()).doesNotContain("Element-100").doesNotContainNull();
    }

    @Test
    void externalRemovalDuringCompactionAtEnd() {
        var set = createIndexedSet();
        for (int i = 0; i < 150; i++) {
            set.add("Element-" + i);
        }

        // Create gaps to trigger compaction
        for (int i = 0; i < 20; i++) {
            set.remove("Element-" + i);
        }

        var processedElements = new ArrayList<String>();
        var counter = new AtomicInteger(0);

        set.forEach(element -> {
            if (counter.incrementAndGet() == 100) {
                // Remove element near the end during compaction
                set.remove("Element-149");
            } else {
                processedElements.add(element);
            }
        });

        assertThat(processedElements).hasSize(129);
        assertThat(set.asList())
                .doesNotContain("Element-149")
                .doesNotContainNull();
    }

    @Test
    void multipleExternalRemovalsDuringCompaction() {
        var set = createIndexedSet();
        for (int i = 0; i < 201; i++) {
            set.add("Element-" + i);
        }

        // Create gaps to trigger compaction (25 gaps = 12.5%)
        var startFrom = 25;
        for (int i = 0; i < startFrom; i++) {
            set.remove("Element-" + i);
        }

        var counter = new AtomicInteger(0);
        var removedExternally = new ArrayList<String>();

        set.forEach(element -> {
            var count = counter.incrementAndGet();
            if (count % 20 == 0) {
                var toRemove = "Element-" + ((count / 10) + startFrom);
                set.remove(toRemove);
                removedExternally.add(toRemove);
            }
        });

        assertThat(set.asList())
                .doesNotContainAnyElementsOf(removedExternally)
                .doesNotContainNull();
    }

    @Test
    void externalRemovalOfElementBeingCompacted() {
        var set = createIndexedSet();
        for (int i = 0; i < 150; i++) {
            set.add("Element-" + i);
        }

        // Create gaps in the middle to trigger compaction
        for (int i = 50; i < 70; i++) {
            set.remove("Element-" + i);
        }

        var processedElements = new ArrayList<String>();

        set.forEach(element -> {
            processedElements.add(element);
            // Try to remove an element that might be moved during compaction
            if (element.equals("Element-70")) {
                set.remove("Element-149"); // Last element, likely to be moved
            }
        });

        assertThat(set.asList()).doesNotContain("Element-149").doesNotContainNull();
    }

    @Test
    void removeAllElementsDuringCompaction() {
        var set = createIndexedSet();
        for (int i = 0; i < 120; i++) {
            set.add("Element-" + i);
        }

        // Create gaps to trigger compaction (15 gaps = 12.5%)
        for (int i = 0; i < 15; i++) {
            set.remove("Element-" + i);
        }

        set.forEach(element -> {
            set.remove(element);
        });

        assertThat(set.isEmpty()).isTrue();
        assertThat(set.size()).isZero();
        assertThat(set.asList()).isEmpty();
    }

    @Test
    void compactionClearsListWhenAllElementsRemoved() {
        var set = createIndexedSet();
        for (int i = 0; i < 100; i++) {
            set.add("Element-" + i);
        }

        // Remove all elements
        for (int i = 0; i < 100; i++) {
            set.remove("Element-" + i);
        }

        // Trigger compaction through forEach
        set.forEach(element -> {
        });

        assertThat(set.isEmpty()).isTrue();
        assertThat(set.size()).isZero();
        assertThat(set.asList()).isEmpty();
    }

    @Test
    void asListForcesCompaction() {
        var set = createIndexedSet();
        for (int i = 0; i < 50; i++) {
            set.add("Element-" + i);
        }

        // Create gaps (but below threshold for forEach compaction)
        for (int i = 0; i < 10; i++) {
            set.remove("Element-" + i);
        }

        // asList should force compaction regardless of threshold
        var list = set.asList();

        assertThat(list)
                .hasSize(40)
                .doesNotContainNull();
    }

    @Test
    void findFirstTriggersCompactionAndReturnsCorrectElement() {
        var set = createIndexedSet();
        for (int i = 0; i < 150; i++) {
            set.add("Element-" + i);
        }

        // Create gaps to trigger compaction
        for (int i = 0; i < 20; i++) {
            set.remove("Element-" + i);
        }

        var found = set.findFirst(element -> element.equals("Element-100"));

        assertThat(found).isEqualTo("Element-100");
        assertThat(set.asList()).hasSize(130).doesNotContainNull();
    }

    @Test
    void findFirstWithExternalRemovalDuringCompaction() {
        var set = createIndexedSet();
        for (int i = 0; i < 150; i++) {
            set.add("Element-" + i);
        }

        // Create gaps to trigger compaction
        for (int i = 0; i < 20; i++) {
            set.remove("Element-" + i);
        }

        var counter = new AtomicInteger(0);
        var found = set.findFirst(element -> {
            if (counter.incrementAndGet() == 10) {
                set.remove("Element-100");
            }
            return element.equals("Element-50");
        });

        assertThat(found).isEqualTo("Element-50");
        assertThat(set.asList()).doesNotContain("Element-100").hasSize(129);
    }

    @Test
    void compactionPreservesElementsCloseToOriginalPosition() {
        var tracker = new CompactingIndexPositionTracker<String>();
        var set = new IndexedSet<>(tracker);

        for (int i = 0; i < 150; i++) {
            set.add("Element-" + i);
        }

        // Remove first 20 elements to create gaps at the beginning
        for (int i = 0; i < 20; i++) {
            set.remove("Element-" + i);
        }

        // Force compaction
        set.asList();

        // Elements from the back should have moved forward
        // The last element (Element-149) should now be at position 0
        assertThat(tracker.clearPosition("Element-149")).isLessThan(20);
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
