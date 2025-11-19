package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
class IndexedSetTest {

    private static IndexedSet<String> createSet() {
        return new IndexedSet<>(new CompactingIndexPositionTracker<>());
    }

    @Nested
    class BasicOperations {

        @Test
        void emptySetHasSizeZero() {
            var set = createSet();
            assertThat(set.isEmpty()).isTrue();
            assertThat(set.size()).isZero();
            assertThat(set.asList()).isEmpty();
        }

        @Test
        void addSingleElement() {
            var set = createSet();
            set.add("A");
            assertThat(set.isEmpty()).isFalse();
            assertThat(set.size()).isEqualTo(1);
            assertThat(set.asList()).containsExactly("A");
        }

        @Test
        void addMultipleElements() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            assertThat(set.size()).isEqualTo(3);
            assertThat(set.asList()).containsExactly("A", "B", "C");
        }

        @Test
        void removeSingleElement() {
            var set = createSet();
            set.add("A");
            set.remove("A");
            assertThat(set.isEmpty()).isTrue();
            assertThat(set.size()).isZero();
            assertThat(set.asList()).isEmpty();
        }

        @Test
        void removeLastElement() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            set.remove("C");
            assertThat(set.size()).isEqualTo(2);
            assertThat(set.asList()).containsExactly("A", "B");
        }

        @Test
        void removeMiddleElement() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            set.remove("B");
            assertThat(set.size()).isEqualTo(2);
            assertThat(set.asList()).containsExactly("A", "C");
        }

        @Test
        void removeFirstElement() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            set.remove("A");
            assertThat(set.size()).isEqualTo(2);
            assertThat(set.asList()).containsExactly("C", "B");
        }

        @Test
        void removeNonExistentElementThrows() {
            var set = createSet();
            set.add("A");
            assertThatThrownBy(() -> set.remove("B"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("was not found");
        }

        @Test
        void removeFromEmptySetThrows() {
            var set = createSet();
            assertThatThrownBy(() -> set.remove("A"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("was not found");
        }

        @Test
        void removeAlreadyRemovedElementThrows() {
            var set = createSet();
            set.add("A");
            set.remove("A");
            assertThatThrownBy(() -> set.remove("A"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("was not found");
        }
    }

    @Nested
    class ForEachTests {

        @Test
        void forEachOnEmptySet() {
            var set = createSet();
            var visited = new ArrayList<String>();
            set.forEach(visited::add);
            assertThat(visited).isEmpty();
        }

        @Test
        void forEachVisitsAllElements() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            var visited = new ArrayList<String>();
            set.forEach(visited::add);
            assertThat(visited).containsExactlyInAnyOrder("A", "B", "C");
        }

        @Test
        void forEachSkipsGaps() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            set.remove("B");
            var visited = new ArrayList<String>();
            set.forEach(visited::add);
            assertThat(visited).containsExactlyInAnyOrder("A", "C");
        }

        @Test
        void forEachWithRemovalDuringIteration() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            set.add("D");
            var visited = new ArrayList<String>();
            set.forEach(element -> {
                visited.add(element);
                if (element.equals("B")) {
                    set.remove("C");
                }
            });
            assertThat(visited).containsExactlyInAnyOrder("A", "B", "D");
            assertThat(set.size()).isEqualTo(3);
        }
    }

    @Nested
    class FindFirstTests {

        @Test
        void findFirstOnEmptySet() {
            var set = createSet();
            assertThat(set.findFirst(s -> true)).isNull();
        }

        @Test
        void findFirstReturnsMatchingElement() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            assertThat(set.findFirst(s -> s.equals("B"))).isEqualTo("B");
        }

        @Test
        void findFirstReturnsNullWhenNoMatch() {
            var set = createSet();
            set.add("A");
            set.add("B");
            assertThat(set.findFirst(s -> s.equals("C"))).isNull();
        }

        @Test
        void findFirstStopsAtFirstMatch() {
            var set = createSet();
            set.add("A");
            set.add("B");
            set.add("C");
            var callCount = new AtomicInteger(0);
            set.findFirst(s -> {
                callCount.incrementAndGet();
                return s.equals("B");
            });
            assertThat(callCount.get()).isLessThanOrEqualTo(2);
        }
    }

    @Nested
    class CompactionTests {

        @Test
        void noCompactionForSmallSets() {
            var set = createSet();
            // Add fewer elements than MINIMUM_ELEMENT_COUNT_FOR_COMPACTION
            for (var i = 0; i < IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION - 1; i++) {
                set.add("Element" + i);
            }
            // Remove half of them to create gaps
            for (var i = 0; i < IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION / 2; i++) {
                set.remove("Element" + i);
            }
            var visited = new ArrayList<String>();
            set.forEach(visited::add);
            // Verify elements are still visited correctly
            assertThat(visited).hasSize(IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION / 2 - 1);
        }

        @Test
        void compactionTriggeredByGapRatio() {
            var set = createSet();
            var elementCount = IndexedSet.MINIMUM_ELEMENT_COUNT_FOR_COMPACTION;
            // Add elements
            for (var i = 0; i < elementCount; i++) {
                set.add("Element" + i);
            }
            // Remove enough elements to exceed GAP_RATIO_FOR_COMPACTION (10%)
            var gapsNeeded = (int) (elementCount * IndexedSet.GAP_RATIO_FOR_COMPACTION) + 1;
            for (var i = 0; i < gapsNeeded; i++) {
                set.remove("Element" + i);
            }
            var visited = new ArrayList<String>();
            set.forEach(visited::add);
            assertThat(visited).hasSize(elementCount - gapsNeeded);
            // After compaction, asList should not trigger further compaction
            assertThat(set.asList()).hasSize(elementCount - gapsNeeded);
        }

        @Test
        void externalRemovalDuringCompaction() {
            var set = createSet();
            var elementCount = 30;
            for (var i = 0; i < elementCount; i++) {
                set.add("Element" + i);
            }
            // Remove elements to trigger compaction
            for (var i = 0; i < 5; i++) {
                set.remove("Element" + i);
            }
            var visited = new ArrayList<String>();
            set.forEach(element -> {
                visited.add(element);
                // Remove an element during iteration
                if (element.equals("Element10")) {
                    set.remove("Element20");
                }
            });
            assertThat(set.size()).isEqualTo(24);
            assertThat(visited).doesNotContain("Element20");
        }

        @Test
        void multipleExternalRemovalsDuringCompaction() {
            var set = createSet();
            for (var i = 0; i < 40; i++) {
                set.add("Element" + i);
            }
            // Create gaps to trigger compaction
            for (var i = 0; i < 6; i++) {
                set.remove("Element" + i);
            }
            var visited = new ArrayList<String>();
            var removed = new ArrayList<String>();
            set.forEach(element -> {
                visited.add(element);
                if (element.equals("Element10")) {
                    set.remove("Element20");
                    removed.add("Element20");
                }
                if (element.equals("Element15")) {
                    set.remove("Element25");
                    removed.add("Element25");
                }
            });
            assertThat(set.size()).isEqualTo(32);
            assertThat(visited).doesNotContainAnyElementsOf(removed);
        }

        @Test
        void removalOfLastElementDuringCompaction() {
            var set = createSet();
            for (var i = 0; i < 30; i++) {
                set.add("Element" + i);
            }
            for (var i = 0; i < 4; i++) {
                set.remove("Element" + i);
            }
            set.forEach(element -> {
                if (element.equals("Element10")) {
                    set.remove("Element29"); // Last element
                }
            });
            assertThat(set.size()).isEqualTo(25);
        }

        @Test
        void clearAllGapsDuringCompaction() {
            var set = createSet();
            for (var i = 0; i < 25; i++) {
                set.add("Element" + i);
            }
            // Remove all elements to create only gaps
            for (var i = 0; i < 25; i++) {
                set.remove("Element" + i);
            }
            assertThat(set.isEmpty()).isTrue();
            assertThat(set.asList()).isEmpty();
        }

        @Test
        void asListTriggersCompaction() {
            var set = createSet();
            for (var i = 0; i < 30; i++) {
                set.add("Element" + i);
            }
            set.remove("Element5");
            set.remove("Element10");
            set.remove("Element15");
            var list = set.asList();
            assertThat(list).hasSize(27);
            assertThat(list).doesNotContainNull();
        }

        @Test
        void forceCompactionWithMultipleGaps() {
            var set = createSet();
            for (var i = 0; i < 50; i++) {
                set.add("Element" + i);
            }
            // Create multiple gaps
            for (var i = 0; i < 10; i += 2) {
                set.remove("Element" + i);
            }
            var list = set.asList();
            assertThat(list).hasSize(45);
            assertThat(list).doesNotContainNull();
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void removeAndReAddCycle() {
            var set = createSet();
            set.add("A");
            set.remove("A");
            set.add("A");
            assertThat(set.size()).isEqualTo(1);
            assertThat(set.asList()).containsExactly("A");
        }

        @Test
        void largeNumberOfElements() {
            var set = createSet();
            var count = 1000;
            for (var i = 0; i < count; i++) {
                set.add("Element" + i);
            }
            assertThat(set.size()).isEqualTo(count);
            assertThat(set.asList()).hasSize(count);
        }

        @Test
        void largeNumberOfGaps() {
            var set = createSet();
            for (var i = 0; i < 100; i++) {
                set.add("Element" + i);
            }
            for (var i = 0; i < 50; i++) {
                set.remove("Element" + (i * 2));
            }
            assertThat(set.size()).isEqualTo(50);
            var list = set.asList();
            assertThat(list).hasSize(50);
            assertThat(list).doesNotContainNull();
        }

        @Test
        void alternatingAddAndRemove() {
            var set = createSet();
            for (var i = 0; i < 100; i++) {
                set.add("Element" + i);
                if (i > 0 && i % 2 == 0) {
                    set.remove("Element" + (i - 1));
                }
            }
            assertThat(set.size()).isGreaterThan(0);
            assertThat(set.asList()).doesNotContainNull();
        }
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
            var result = positionMap.remove(element);
            return result != null ? result : -1;
        }

    }
}
