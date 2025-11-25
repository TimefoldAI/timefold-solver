package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@NullMarked
class ElementAwareArrayListTest {

    @Nested
    @DisplayName("Basic operations")
    class BasicOperations {

        @Test
        @DisplayName("Empty list has size 0 and isEmpty returns true")
        void emptyList() {
            var list = new ElementAwareArrayList<String>();

            assertThat(list.isEmpty()).isTrue();
            assertThat(list.size()).isZero();
            assertThat(list.asList()).isEmpty();
        }

        @Test
        @DisplayName("Add single element increases size")
        void addSingleElement() {
            var list = new ElementAwareArrayList<String>();

            var entry = list.add("first");

            assertThat(list.isEmpty()).isFalse();
            assertThat(list.size()).isEqualTo(1);
            assertThat(entry.getElement()).isEqualTo("first");
            assertThat(entry.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("Add multiple elements maintains insertion order")
        void addMultipleElements() {
            var list = new ElementAwareArrayList<String>();

            var entry1 = list.add("first");
            var entry2 = list.add("second");
            var entry3 = list.add("third");

            assertThat(list.size()).isEqualTo(3);
            assertThat(list.asList())
                    .containsExactly(entry1, entry2, entry3);
        }

        @Test
        @DisplayName("Remove single element from middle creates gap")
        void removeSingleElement() {
            var list = new ElementAwareArrayList<String>();

            var entry1 = list.add("first");
            var entry2 = list.add("second");
            var entry3 = list.add("third");

            list.remove(entry2);

            assertThat(list.size()).isEqualTo(2);
            assertThat(entry2.isRemoved()).isTrue();
            assertThat(entry1.isRemoved()).isFalse();
            assertThat(entry3.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("Remove already removed element throws exception")
        void removeAlreadyRemovedElement() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.add("first");

            list.remove(entry);

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> list.remove(entry))
                    .withMessageContaining("was already removed");
        }

        @Test
        @DisplayName("Remove all elements makes list empty")
        void removeAllElements() {
            var list = new ElementAwareArrayList<String>();

            var entry1 = list.add("first");
            var entry2 = list.add("second");

            list.remove(entry1);
            list.remove(entry2);

            assertThat(list.isEmpty()).isTrue();
            assertThat(list.size()).isZero();
        }
    }

    @Nested
    @DisplayName("forEach tests")
    class ForEachTests {

        @Test
        @DisplayName("forEach on empty list does nothing")
        void forEachOnEmptyList() {
            var list = new ElementAwareArrayList<String>();
            var counter = new AtomicInteger(0);

            list.forEach(s -> counter.incrementAndGet());

            assertThat(counter.get()).isZero();
        }

        @Test
        @DisplayName("forEach without gaps processes all elements")
        void forEachWithoutGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("first");
            list.add("second");
            list.add("third");

            var result = new ArrayList<String>();
            list.forEach(result::add);

            assertThat(result).containsExactly("first", "second", "third");
        }

        @Test
        @DisplayName("forEach with gaps processes only non-removed elements")
        void forEachWithGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("first");
            var entry2 = list.add("second");
            list.add("third");

            list.remove(entry2);

            var result = new ArrayList<String>();
            list.forEach(result::add);

            assertThat(result).containsExactly("first", "third");
        }

        @Test
        @DisplayName("forEach compacts the list when gaps exist")
        void forEachCompactsWithGaps() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.add("first");
            var entry2 = list.add("second");
            var entry3 = list.add("third");
            var entry4 = list.add("fourth");

            list.remove(entry2);
            list.remove(entry3);

            list.forEach(s -> {
            });

            // After compaction, only non-removed elements remain
            assertThat(list.asList()).containsExactly(entry1, entry4);
            assertThat(list.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("forEach clears list when all elements are removed")
        void forEachClearsWhenAllRemoved() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.add("first");
            var entry2 = list.add("second");

            list.remove(entry1);
            list.remove(entry2);

            list.forEach(s -> {
            });

            assertThat(list.isEmpty()).isTrue();
            assertThat(list.asList()).isEmpty();
        }

        @Test
        @DisplayName("forEach compacts when tail gaps are encountered")
        void forEachCompactsWithTailGaps() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.add("first");
            var entry2 = list.add("second");
            var entry3 = list.add("third");
            var entry4 = list.add("fourth");

            list.remove(entry3);
            list.remove(entry4);

            var result = new ArrayList<String>();
            list.forEach(result::add);

            assertThat(result).containsExactly("first", "second");
            assertThat(list.asList()).containsExactly(entry1, entry2);
        }
    }

    @Nested
    @DisplayName("asList tests")
    class AsListTests {

        @Test
        @DisplayName("asList returns empty list when list is empty")
        void asListWhenEmpty() {
            var list = new ElementAwareArrayList<String>();

            assertThat(list.asList()).isEmpty();
        }

        @Test
        @DisplayName("asList returns all entries when no gaps")
        void asListWithoutGaps() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.add("first");
            var entry2 = list.add("second");

            assertThat(list.asList()).containsExactly(entry1, entry2);
        }

        @Test
        @DisplayName("asList compacts the list when gaps exist")
        void asListCompactsWithGaps() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.add("first");
            var entry2 = list.add("second");
            var entry3 = list.add("third");

            list.remove(entry2);

            var result = list.asList();

            assertThat(result).containsExactly(entry1, entry3);
            assertThat(list.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("asList returns empty when all elements removed")
        void asListWhenAllRemoved() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.add("first");

            list.remove(entry1);

            assertThat(list.asList()).isEmpty();
        }

        @Test
        @DisplayName("asList compacts with tail gaps")
        void asListCompactsWithTailGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("first");
            list.add("second");
            var entry3 = list.add("third");
            var entry4 = list.add("fourth");
            var entry5 = list.add("fifth");

            list.remove(entry3);
            list.remove(entry4);
            list.remove(entry5);

            assertThat(list.asList())
                    .hasSize(2)
                    .doesNotContain(entry3, entry4, entry5);
        }
    }

    @Nested
    @DisplayName("Entry tests")
    class EntryTests {

        @Test
        @DisplayName("Entry getElement returns correct element")
        void entryGetElement() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.add("test");

            assertThat(entry.getElement()).isEqualTo("test");
        }

        @Test
        @DisplayName("Entry isRemoved returns false for active entry")
        void entryIsRemovedFalse() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.add("test");

            assertThat(entry.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("Entry isRemoved returns true after removal")
        void entryIsRemovedTrue() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.add("test");

            list.remove(entry);

            assertThat(entry.isRemoved()).isTrue();
        }

        @Test
        @DisplayName("Entry toString shows element and position when active")
        void entryToStringActive() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.add("test");

            assertThat(entry).hasToString("test@0");
        }

        @Test
        @DisplayName("Entry toString shows null when removed")
        void entryToStringRemoved() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.add("test");

            list.remove(entry);

            assertThat(entry).hasToString("null");
        }

    }

    @Nested
    @DisplayName("Complex scenarios")
    class ComplexScenarios {

        @Test
        @DisplayName("Multiple additions and removals maintain correct state")
        void multipleOperations() {
            var list = new ElementAwareArrayList<Integer>();

            list.add(1);
            var e2 = list.add(2);
            list.add(3);
            var e4 = list.add(4);
            list.add(5);

            list.remove(e2);
            list.remove(e4);

            assertThat(list.size()).isEqualTo(3);

            var result = new ArrayList<Integer>();
            list.forEach(result::add);

            assertThat(result).containsExactly(1, 3, 5);
        }

        @Test
        @DisplayName("Remove from head, middle, and tail")
        void removeFromVariousPositions() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.add("a");
            list.add("b");
            var e3 = list.add("c");
            list.add("d");
            var e5 = list.add("e");

            list.remove(e1); // head
            list.remove(e3); // middle
            list.remove(e5); // tail

            var result = new ArrayList<String>();
            list.forEach(result::add);

            assertThat(result).containsExactly("b", "d");
            assertThat(list.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("Interleaved adds and removes")
        void interleavedOperations() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.add("1");
            var e2 = list.add("2");
            list.remove(e1);
            list.add("3");
            list.remove(e2);
            list.add("4");

            assertThat(list.size()).isEqualTo(2);

            var result = new ArrayList<String>();
            list.forEach(result::add);

            assertThat(result).containsExactly("3", "4");
        }

        @Test
        @DisplayName("Large list with many gaps compacts correctly")
        void largeListWithManyGaps() {
            var list = new ElementAwareArrayList<Integer>();
            var entries = new ArrayList<ElementAwareArrayList.Entry<Integer>>();

            // Add 100 elements
            for (int i = 0; i < 100; i++) {
                entries.add(list.add(i));
            }

            // Remove every other element
            for (int i = 0; i < 100; i += 2) {
                list.remove(entries.get(i));
            }

            assertThat(list.size()).isEqualTo(50);

            var result = new ArrayList<Integer>();
            list.forEach(result::add);

            assertThat(result).hasSize(50);
            for (int i = 0; i < 50; i++) {
                assertThat(result.get(i)).isEqualTo(i * 2 + 1);
            }
        }

        @Test
        @DisplayName("Remove all then add new elements")
        void removeAllThenAddNew() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.add("old1");
            var e2 = list.add("old2");

            list.remove(e1);
            list.remove(e2);

            assertThat(list.isEmpty()).isTrue();

            list.add("new1");
            list.add("new2");

            assertThat(list.size()).isEqualTo(2);

            var result = new ArrayList<String>();
            list.forEach(result::add);

            assertThat(result).containsExactly("new1", "new2");
        }

        @Test
        @DisplayName("Multiple forEach calls maintain correct state")
        void multipleForEachCalls() {
            var list = new ElementAwareArrayList<String>();

            list.add("a");
            var e2 = list.add("b");
            list.add("c");

            list.remove(e2);

            var result1 = new ArrayList<String>();
            list.forEach(result1::add);

            var result2 = new ArrayList<String>();
            list.forEach(result2::add);

            assertThat(result1).containsExactly("a", "c");
            assertThat(result2).containsExactly("a", "c");
            assertThat(list.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("Position tracking after compaction")
        void positionTrackingAfterCompaction() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.add("a");
            var e2 = list.add("b");
            var e3 = list.add("c");
            var e4 = list.add("d");

            list.remove(e2);

            list.forEach(s -> {
            });

            // After compaction, positions should be adjusted
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@1");
            assertThat(e4.toString()).contains("@2");
        }
    }

}