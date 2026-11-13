package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@NullMarked
class ElementAwareArrayListTest {

    @Nested
    @DisplayName("Basic operations")
    class BasicOperations {

        @Test
        @DisplayName("Empty list has size 0 and isEmpty returns true")
        void emptyList() {
            var list = new ElementAwareArrayList<String>();

            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("Add single element increases size")
        void addSingleElement() {
            var list = new ElementAwareArrayList<String>();

            var entry = list.addEntry("first");

            assertThat(list)
                    .isNotEmpty()
                    .hasSize(1);
            assertThat(entry.element()).isEqualTo("first");
            assertThat(entry.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("Add multiple elements maintains insertion order")
        void addMultipleElements() {
            var list = new ElementAwareArrayList<String>();

            list.addEntry("first");
            list.addEntry("second");
            list.addEntry("third");

            assertThat(list).containsExactly("first", "second", "third");
        }

        @Test
        @DisplayName("Remove single element from middle creates gap")
        void removeSingleElement() {
            var list = new ElementAwareArrayList<String>();

            var entry1 = list.addEntry("first");
            var entry2 = list.addEntry("second");
            var entry3 = list.addEntry("third");

            entry2.remove();

            assertThat(list).hasSize(2);
            assertThat(entry2.isRemoved()).isTrue();
            assertThat(entry1.isRemoved()).isFalse();
            assertThat(entry3.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("Remove already removed element throws exception")
        void removeAlreadyRemovedElement() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("first");

            entry.remove();

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(entry::remove)
                    .withMessageContaining("was already removed");
        }

        @Test
        @DisplayName("Remove all elements makes list empty")
        void removeAllElements() {
            var list = new ElementAwareArrayList<String>();

            var entry1 = list.addEntry("first");
            var entry2 = list.addEntry("second");

            entry1.remove();
            entry2.remove();

            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("addEntry after churn appends in insertion order")
        void addEntryAfterChurnAppendsInOrder() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var entryB = list.addEntry("b");
            var entryC = list.addEntry("c");

            entryB.remove(); // interior gap at slot 1
            entryC.remove(); // last element: trim + retract trailing null → lastElementPosition=0, gapCount=0

            // addEntry appends at lastElementPosition+1 (==1); insertion order a, x preserved.
            var entryX = list.addEntry("x");

            assertThat(list).containsExactly("a", "x");
            assertThat(entryX.toString()).contains("@1");
        }

        @Test
        @DisplayName("add after free-path empty re-allocates and preserves insertion order")
        void addAfterLargeArrayFreePathPreservesOrder() {
            var list = new ElementAwareArrayList<String>();
            List<ElementAwareArrayList<String>.Entry> entryList = new ArrayList<>();
            for (var i = 0; i < 30; i++) {
                entryList.add(list.addEntry("e" + i));
            }
            for (var entry : entryList) {
                entry.remove();
            }
            list.addEntry("x");
            list.addEntry("y");
            list.addEntry("z");
            assertThat(list).containsExactly("x", "y", "z");
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

            assertThat(copyUsingForEach(list)).containsExactly("first", "second", "third");
        }

        @Test
        @DisplayName("forEach with gaps processes only non-removed elements")
        void forEachWithGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("first");
            var entry2 = list.addEntry("second");
            list.add("third");

            entry2.remove();

            assertThat(copyUsingForEach(list)).containsExactly("first", "third");
        }

        @Test
        @DisplayName("forEach compacts the list when gaps exist")
        void forEachCompactsWithGaps() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.addEntry("first");
            var entry2 = list.addEntry("second");
            var entry3 = list.addEntry("third");
            var entry4 = list.addEntry("fourth");

            entry2.remove();
            entry3.remove();

            list.forEach(s -> {
            });

            assertThat(list).hasSize(2);
            assertThat(entry1.toString()).contains("@0");
            assertThat(entry4.toString()).contains("@1");
        }

        @Test
        @DisplayName("forEach clears list when all elements are removed")
        void forEachClearsWhenAllRemoved() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.addEntry("first");
            var entry2 = list.addEntry("second");

            entry1.remove();
            entry2.remove();

            list.forEach(s -> {
            });

            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("forEach after trailing removals: remove trims them, so no gap remains")
        void forEachAfterTrailingRemovals() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.addEntry("first");
            var entry2 = list.addEntry("second");
            var entry3 = list.addEntry("third");
            var entry4 = list.addEntry("fourth");

            entry3.remove();
            entry4.remove();

            assertThat(copyUsingForEach(list)).containsExactly("first", "second");
            assertThat(list).hasSize(2);
            assertThat(entry1.toString()).contains("@0");
            assertThat(entry2.toString()).contains("@1");
        }
    }

    @Nested
    @DisplayName("Gapped read tests")
    class GappedReadTests {

        @Test
        @DisplayName("Gaps before target: get() resolves past them without relocating anything")
        void getGapsBeforeTargetDoesNotRelocate() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 20);

            entryList.get(1).remove();
            entryList.get(3).remove();
            // gapCount(2) stays under a quarter of size(18), so the gaps survive for this test to observe.
            assertThat(list.slotCount()).isEqualTo(20);

            assertThat(list.get(1)).isEqualTo("e2"); // The gap at slot 1 is skipped.
            assertThat(list.get(2)).isEqualTo("e4"); // Both gaps are skipped.
            assertThat(list).hasSize(18);

            // Reads never move anything: every surviving entry keeps the slot it was created at.
            assertThat(entryList.get(2).toString()).contains("@2");
            assertThat(entryList.get(4).toString()).contains("@4");
            assertThat(entryList.get(19).toString()).contains("@19");
            assertThat(list.slotCount()).isEqualTo(20);

            // A suffix entry is still removable through its unchanged position.
            entryList.get(19).remove();
            assertThat(list).hasSize(17);
            assertThat(copyUsingForEach(list)).doesNotContain("e19");
        }

        @Test
        @DisplayName("Gaps before target (at last slot): get resolves around the gap")
        void getGapsBeforeTarget_lastSlot() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");

            e2.remove();
            // Physical: [a, null, c], gapCount=1, lastElementPosition=2, size=2

            assertThat(list.get(1)).isEqualTo("c");
            assertThat(e3.toString()).contains("@2");
            assertThat(list).hasSize(2);
            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(1)).isEqualTo("c");
        }

        @Test
        @DisplayName("Trailing removal leaves no gap; the read moves nothing")
        void getAfterTrailingRemoval() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");

            e3.remove();
            // Removing the last element trims it: [a, b], gapCount=0.

            list.getFirst();
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@1"); // unchanged
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("Target is last logical element, after removing the trailing elements")
        void getLastElement_trailingGapsWithNoPrefix() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e3.remove();
            e4.remove();
            // Removing the last element trims trailing gaps: [a, b], gapCount=0.

            list.get(1);
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@1");
            assertThat(list).hasSize(2);
            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(1)).isEqualTo("b");
        }

        @Test
        @DisplayName("Target is last logical element, a gap before it: get resolves around the gap")
        void getLastElement_gapBefore() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e1.remove();
            e3.remove();
            e4.remove();
            // Physical: [null, b], gapCount=1, lastElementPosition=1, size=1
            assertThat(list.slotCount()).isEqualTo(2);

            assertThat(list.getFirst()).isEqualTo("b");
            assertThat(e2.toString()).contains("@1");
            assertThat(list).hasSize(1);
        }

        @Test
        @DisplayName("Gaps before and after target: neither side is disturbed by the read")
        void getGapsMixedAroundTarget() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 20);

            entryList.get(1).remove(); // Before the target.
            entryList.get(15).remove(); // After the target.
            assertThat(list.slotCount()).isEqualTo(20);

            assertThat(list.get(5)).isEqualTo("e6"); // One gap ahead of it, one behind it.

            // Entries on both sides of the target keep their slots.
            assertThat(entryList.get(6).toString()).contains("@6");
            assertThat(entryList.get(19).toString()).contains("@19");
            assertThat(list).hasSize(18);
            assertThat(copyUsingForEach(list)).doesNotContain("e1", "e15");
        }

        @Test
        @DisplayName("Entry found by get around a gap keeps its position for O(1) removal")
        void removeEntryFoundAroundAGap() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            list.addEntry("d");

            e2.remove();
            // Physical: [a, null, c, d], gapCount=1, size=3

            assertThat(list.get(1)).isEqualTo("c");
            assertThat(e3.toString()).contains("@2");

            e3.remove(); // Uses its unchanged position.
            assertThat(e3.isRemoved()).isTrue();
            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("a", "d");
        }

        @Test
        @DisplayName("Entry after a read keeps its position for removal")
        void removeSuffixEntry_positionUnchanged() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e2.remove();
            e4.remove();
            // Physical: [a, null, c, null, e], gapCount=2, size=3

            list.get(1); // Reads around the gaps; moves nothing.

            e5.remove(); // Uses its unchanged position 4.
            assertThat(e5.isRemoved()).isTrue();
            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("a", "c");
        }

        @Test
        @DisplayName("Sequential get calls resolve around the gaps without moving entries")
        void sequentialGet_entriesStayInPlace() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e2.remove();
            e4.remove();
            // Physical: [a, null, c, null, e], gapCount=2, size=3

            assertThat(list.get(1)).isEqualTo("c");
            assertThat(e3.toString()).contains("@2");
            assertThat(list.get(2)).isEqualTo("e");
            assertThat(e5.toString()).contains("@4");
            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("a", "c", "e");
        }

        @Test
        @DisplayName("forEach fully compacts a gappy list after a read")
        void getThenForEach_fullCompactCorrect() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e2.remove();
            e4.remove();
            // Physical: [a, null, c, null, e], gapCount=2

            list.get(1); // Reads around the gaps; moves nothing.

            assertThat(copyUsingForEach(list)).containsExactly("a", "c", "e");
            // After forEach: fully compacted
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@1");
            assertThat(e5.toString()).contains("@2");
            assertThat(list).hasSize(3);
        }

        @Test
        @DisplayName("addAt after a read compacts, then shifts; all positions correct")
        void getThenAddAt_fullCompactCorrect() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e2.remove();
            // Physical: [a, null, c, d], gapCount=1, size=3

            assertThat(list.get(1)).isEqualTo("c"); // Reads around the gap; moves nothing.

            list.add(1, "x"); // Compacts, then shifts c and d up by one.
            assertThat(list).hasSize(4);
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "c", "d");
            // add(int, E) compacted the list; entry positions are dense.
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@2");
            assertThat(e4.toString()).contains("@3");
            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(2)).isEqualTo("c");
            assertThat(list.get(3)).isEqualTo("d");
        }

        @Test
        @DisplayName("get on index >= size throws IndexOutOfBoundsException")
        void getOutOfBounds_throwsIOOB() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            list.addEntry("c");

            e2.remove();
            // size=2

            assertThatExceptionOfType(IndexOutOfBoundsException.class)
                    .isThrownBy(() -> list.get(2));
        }
    }

    @Nested
    @DisplayName("Entry tests")
    class EntryTests {

        @Test
        @DisplayName("Entry getElement returns correct element")
        void entryGetElement() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("test");

            assertThat(entry.element()).isEqualTo("test");
        }

        @Test
        @DisplayName("Entry isRemoved returns false for active entry")
        void entryIsRemovedFalse() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("test");

            assertThat(entry.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("Entry isRemoved returns true after removal")
        void entryIsRemovedTrue() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("test");

            entry.remove();

            assertThat(entry.isRemoved()).isTrue();
        }

        @Test
        @DisplayName("Entry toString shows element and position when active")
        void entryToStringActive() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("test");

            assertThat(entry).hasToString("test@0");
        }

        @Test
        @DisplayName("Entry toString shows null when removed")
        void entryToStringRemoved() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("test");

            entry.remove();

            assertThat(entry).hasToString("null");
        }

    }

    @Nested
    @DisplayName("Null element support")
    class NullElementTests {

        @Test
        @DisplayName("addEntry(null) and get(0) round-trip")
        void addAndGet() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry(null);
            assertThat(list).hasSize(1);
            assertThat(list.getFirst()).isNull();
        }

        @Test
        @DisplayName("forEach visits null elements")
        void forEach() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry("a");
            list.addEntry(null);
            list.addEntry("b");
            assertThat(copyUsingForEach(list)).containsExactly("a", null, "b");
        }

        @Test
        @DisplayName("forEach with gaps visits null elements")
        void forEachWithGaps() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry("a");
            var e2 = list.addEntry("x");
            list.addEntry(null);
            list.addEntry("b");
            e2.remove();
            assertThat(copyUsingForEach(list)).containsExactly("a", null, "b");
        }

        @Test
        @DisplayName("remove(Entry) of null element works")
        void removeNullEntry() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry("a");
            var nullEntry = list.addEntry(null);
            list.addEntry("b");
            nullEntry.remove();
            assertThat(copyUsingForEach(list)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("listIterator visits null elements in forward and backward order")
        void iterator() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry(null);
            list.addEntry("a");
            list.addEntry(null);

            // Forward traversal returns null elements.
            var it = list.listIterator();
            assertThat(it.next()).isNull();
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isNull();
            assertThat(it.hasNext()).isFalse();

            // Backward traversal returns null elements.
            assertThat(it.previous()).isNull();
            assertThat(it.previous()).isEqualTo("a");
            assertThat(it.previous()).isNull();
            assertThat(it.hasPrevious()).isFalse();

            // remove() creates a gap (Entry == null) adjacent to a live null element (Entry != null, element == null);
            // next() must skip the former without skipping the latter.
            it = list.listIterator();
            assertThat(it.next()).isNull(); // null element at logical 0
            it.remove(); // slot 0 becomes a removed gap; live null element remains at slot 2
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isNull(); // live null element still reachable
            assertThat(it.hasNext()).isFalse();
            assertThat(list).containsExactly("a", null);
        }

        @Test
        @DisplayName("entry.element() returns null for null element")
        void entryElement() {
            var list = new ElementAwareArrayList<@Nullable String>();
            var entry = list.addEntry(null);
            assertThat(entry.element()).isNull();
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
            var e2 = list.addEntry(2);
            list.add(3);
            var e4 = list.addEntry(4);
            list.add(5);

            e2.remove();
            e4.remove();

            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly(1, 3, 5);
        }

        @Test
        @DisplayName("Remove from head, middle, and tail")
        void removeFromVariousPositions() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.addEntry("a");
            list.add("b");
            var e3 = list.addEntry("c");
            list.add("d");
            var e5 = list.addEntry("e");

            e1.remove(); // head
            e3.remove(); // middle
            e5.remove(); // tail

            assertThat(copyUsingForEach(list)).containsExactly("b", "d");
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("Interleaved adds and removes")
        void interleavedOperations() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.addEntry("1");
            var e2 = list.addEntry("2");
            e1.remove();
            list.add("3");
            e2.remove();
            list.add("4");

            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("3", "4");
        }

        @Test
        @DisplayName("Large list with many gaps compacts correctly")
        void largeListWithManyGaps() {
            var list = new ElementAwareArrayList<Integer>();
            var entries = new ArrayList<ElementAwareArrayList<Integer>.Entry>();

            // Add 100 elements
            for (var i = 0; i < 100; i++) {
                entries.add(list.addEntry(i));
            }

            // Remove every other element
            for (var i = 0; i < 100; i += 2) {
                entries.get(i).remove();
            }

            assertThat(list).hasSize(50);

            var result = copyUsingForEach(list);
            assertThat(result).hasSize(50);
            for (var i = 0; i < 50; i++) {
                assertThat(result.get(i)).isEqualTo(i * 2 + 1);
            }
        }

        @Test
        @DisplayName("Remove all then add new elements")
        void removeAllThenAddNew() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.addEntry("old1");
            var e2 = list.addEntry("old2");

            e1.remove();
            e2.remove();

            assertThat(list).isEmpty();

            list.add("new1");
            list.add("new2");

            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("new1", "new2");
        }

        @Test
        @DisplayName("Multiple forEach calls maintain correct state")
        void multipleForEachCalls() {
            var list = new ElementAwareArrayList<String>();

            list.add("a");
            var e2 = list.addEntry("b");
            list.add("c");

            e2.remove();

            assertThat(copyUsingForEach(list)).containsExactly("a", "c");
            assertThat(copyUsingForEach(list)).containsExactly("a", "c");
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("Position tracking after compaction")
        void positionTrackingAfterCompaction() {
            var list = new ElementAwareArrayList<String>();

            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e2.remove();

            list.forEach(s -> {
            });

            // After compaction, positions should be adjusted
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@1");
            assertThat(e4.toString()).contains("@2");
        }
    }

    @Nested
    @DisplayName("add(int, Object) tests")
    class AddAtIndexTests {

        @Test
        @DisplayName("addAt at end is equivalent to add")
        void addAtEnd() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");

            list.add(2, "c");
            assertThat(list).hasSize(3);
            assertThat(list.get(2)).isEqualTo("c");
            assertThat(copyUsingForEach(list)).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("addAt at beginning shifts existing entries")
        void addAtBeginning() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("b");
            var e2 = list.addEntry("c");

            list.add(0, "a");

            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("a", "b", "c");
            assertThat(e1.isRemoved()).isFalse();
            assertThat(e2.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("addAt in middle shifts subsequent entries")
        void addAtMiddle() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("c");

            list.add(1, "b");

            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("addAt on a list with a leading gap compacts first")
        void addAtCompactsFirst() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            list.add("b");
            var e3 = list.addEntry("c");

            e1.remove();

            list.add(1, "x");

            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("b", "x", "c");
            assertThat(e3.isRemoved()).isFalse();
        }

        @Test
        @DisplayName("addAt at a gap slot compacts, then shifts")
        void addAtFillsGap() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e2.remove();
            // Physical: [a@0, null, c@2, d@3], gapCount=1

            list.add(1, "x");
            // Compacts (c to 1, d to 2), then shifts c and d up by one.

            assertThat(list).hasSize(4);
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@2"); // Compacted to slot 1, then shifted to slot 2.
            assertThat(e4.toString()).contains("@3");
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "c", "d");
        }

        @Test
        @DisplayName("addAt on a gapped list compacts first, leaving dense positions")
        void addAtCompactsGappedList() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e1.remove();
            e3.remove();
            // Physical: [null, b@1, null, d@3], gapCount=2

            list.add(1, "x"); // A relocating write, so it compacts to [b, d] and then shifts d right.

            assertThat(list).hasSize(3);
            assertThat(e2.toString()).contains("@0");
            assertThat(e4.toString()).contains("@2"); // Dense: no gap survives the insert.
            assertThat(list.slotCount()).isEqualTo(3);
            assertThat(copyUsingForEach(list)).containsExactly("b", "x", "d");
        }

        @Test
        @DisplayName("addAt before a later gap compacts, then shifts")
        void addAtRotatesIntoGap() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e4.remove();
            // Physical: [a@0, b@1, c@2, null, e@4], gapCount=1

            list.add(1, "x");
            // Compacts (e to 3), then shifts b, c and e up by one.

            assertThat(list).hasSize(5);
            // The gap is gone; the slot count is unchanged.
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@2"); // b shifted to slot 2
            assertThat(e3.toString()).contains("@3"); // c shifted to slot 3
            assertThat(e5.toString()).contains("@4"); // e compacted to slot 3, then shifted to slot 4
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "b", "c", "e");
        }

        @Test
        @DisplayName("addAt with negative index throws IndexOutOfBoundsException")
        void addAtNegativeIndex() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");

            assertThatExceptionOfType(IndexOutOfBoundsException.class)
                    .isThrownBy(() -> list.add(-1, "x"));
        }

        @Test
        @DisplayName("addAt beyond size throws IndexOutOfBoundsException")
        void addAtBeyondSize() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");

            assertThatExceptionOfType(IndexOutOfBoundsException.class)
                    .isThrownBy(() -> list.add(2, "x"));
        }

        @Test
        @DisplayName("addAt beyond logical size on gappy list does not invalidate iterator")
        void addAtBeyondLogicalSizeOnGappyList() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entry = list.addEntry("b");
            list.add("c");
            entry.remove();
            // size = 2, slotCount = 3

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");

            assertThatExceptionOfType(IndexOutOfBoundsException.class)
                    .isThrownBy(() -> list.add(3, "x"));
            assertThatCode(() -> assertThat(it.next()).isEqualTo("c"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("addAt into empty list at index 0")
        void addAtEmptyList() {
            var list = new ElementAwareArrayList<String>();

            list.add(0, "a");

            assertThat(list).hasSize(1);
            assertThat(list.getFirst()).isEqualTo("a");
        }

        @Test
        @DisplayName("existing entries remain removable after addAt")
        void existingEntriesRemovableAfterAddAt() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            list.add("c");
            list.add(1, "b");
            e1.remove();

            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("b", "c");
        }

        @Test
        @DisplayName("addAt consumes every gap, not just the nearest one")
        void addAtConsumesAllGaps() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e3.remove();
            e4.remove();
            // Physical: [a@0, b@1, null, null, e@4], gapCount=2

            list.add(1, "x"); // Compacts to [a, b, e], then shifts b and e right to make room.

            assertThat(list).hasSize(4);
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@2");
            assertThat(e5.toString()).contains("@3"); // Both gaps are gone, so e moved down.
            assertThat(list.slotCount()).isEqualTo(4);
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "b", "e");
        }
    }

    @Nested
    @DisplayName("ListIterator tests")
    class ListIteratorTests {

        @Test
        @DisplayName("forward iteration over gap-free list")
        void forwardIteration() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator();
            assertThat(it.hasNext()).isTrue();
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isEqualTo("b");
            assertThat(it.next()).isEqualTo("c");
            assertThat(it.hasNext()).isFalse();
        }

        @Test
        @DisplayName("backward iteration over gap-free list")
        void backwardIteration() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator(3);
            assertThat(it.hasPrevious()).isTrue();
            assertThat(it.previous()).isEqualTo("c");
            assertThat(it.previous()).isEqualTo("b");
            assertThat(it.previous()).isEqualTo("a");
            assertThat(it.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("remove after next() removes correct element, adjusts cursor")
        void removeFwd() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");
            it.remove();
            assertThat(it.hasPrevious()).isFalse();
            assertThat(it).hasNext();
            assertThat(it.next()).isEqualTo("b");
            assertThat(list).containsExactly("b", "c");
        }

        @Test
        @DisplayName("remove after previous() removes correct element; cursor ping-pong")
        void removeBwd() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator();
            it.next();
            it.next();
            it.next();
            assertThat(it.previous()).isEqualTo("c");
            it.remove();
            assertThat(list).containsExactly("a", "b");
            assertThat(it.hasNext()).isFalse();
            assertThat(it.hasPrevious()).isTrue();
            assertThat(it.previous()).isEqualTo("b");
            assertThat(it.next()).isEqualTo("b");
        }

        @Test
        @DisplayName("set() replaces element without invalidating concurrent iterators")
        void set() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");

            var it = list.listIterator();
            var it2 = list.listIterator();
            it.next();
            it.set("x");
            assertThat(list).containsExactly("x", "b");

            assertThat(it2.next()).isEqualTo("x");
        }

        @Test
        @DisplayName("add() inserts before next element; next() unaffected, previous() returns new element")
        void addAtCursor() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");
            it.add("x");
            assertThat(it.next()).isEqualTo("b");
            assertThat(it.previous()).isEqualTo("b");
            assertThat(it.previous()).isEqualTo("x");
            assertThat(list).containsExactly("a", "x", "b", "c");
        }

        @Test
        @DisplayName("add() on gappy list compacts then inserts at correct logical position")
        void addWithGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entry = list.addEntry("b");
            list.add("c");
            entry.remove();

            var it = list.listIterator(1);
            it.add("x");
            assertThat(list).containsExactly("a", "x", "c");
        }

        @Test
        @DisplayName("add() appending to list made gappy mid-iteration positions cursor for previous()")
        void addAtEndAfterMidIterationRemoval() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");
            list.add("d");

            var it = list.listIterator();
            it.next(); // a
            it.next(); // b
            it.remove(); // gap at physical 1; logical [a, c, d]
            it.next(); // c
            it.next(); // d -> logical end, interior gap still present
            it.add("e"); // append via addEntry while gap exists

            assertThat(it.hasNext()).isFalse();
            assertThat(it.previous()).isEqualTo("e");
            assertThat(it.previous()).isEqualTo("d");
            assertThat(it.previous()).isEqualTo("c");
            assertThat(list).containsExactly("a", "c", "d", "e");
        }

        @Test
        @DisplayName("next() past end throws NoSuchElementException")
        void noSuchElement() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");

            var it = list.listIterator();
            it.next();
            assertThatExceptionOfType(NoSuchElementException.class)
                    .isThrownBy(it::next);
        }

        @Test
        @DisplayName("remove() without preceding next() or previous() throws IllegalStateException")
        void removeWithoutNext() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");

            var it = list.listIterator();
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(it::remove);
        }

        @Test
        @DisplayName("listIterator(index) starts cursor after given logical position")
        void startAtIndex() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator(2);
            assertThat(it.next()).isEqualTo("c");
            assertThat(it.hasNext()).isFalse();

            it = list.listIterator(2);
            assertThat(it.previous()).isEqualTo("b");
        }

        @Test
        @DisplayName("iterator remove on list that already has a gap")
        void removeViaIteratorOnGappyList() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entryB = list.addEntry("b");
            list.add("c");
            list.add("d");
            entryB.remove();
            // listIterator() does not compact, so the b-gap is still present during iteration.

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isEqualTo("c");
            it.remove(); // removes "c" from already-gappy list

            assertThat(list).hasSize(2);
            assertThat(it.hasNext()).isTrue();
            assertThat(it.hasPrevious()).isTrue();
            assertThat(it.next()).isEqualTo("d");
            assertThat(it.hasNext()).isFalse();
            assertThat(copyUsingForEach(list)).containsExactly("a", "d");
        }

        @Test
        @DisplayName("next-remove-next-previous cycle with a pre-existing gap")
        void nextRemoveNextPreviousCycle() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entryX = list.addEntry("x");
            list.add("b");
            list.add("c");
            entryX.remove();
            // listIterator() does not compact, so the x-gap is still present during iteration.

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isEqualTo("b");
            it.remove(); // logical: [a, c]

            assertThat(it.next()).isEqualTo("c");
            assertThat(it.previous()).isEqualTo("c");
            assertThat(it.previous()).isEqualTo("a");
            assertThat(it.hasPrevious()).isFalse();
            assertThat(list).containsExactly("a", "c");
        }

        @Test
        @DisplayName("removing all elements via iterator produces no spurious CME")
        void removeLastElementNoDoubleCme() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");
            list.add("c");

            var it = list.listIterator();
            while (it.hasNext()) {
                it.next();
                it.remove();
            }
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("set() without preceding next() or previous() throws IllegalStateException")
        void setWithoutNextOrPrevious() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");

            var it = list.listIterator();
            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(() -> it.set("x"));
        }

    }

    @Nested
    @DisplayName("compact() tests")
    class CompactTests {

        @Test
        @DisplayName("compact() removes all gaps and preserves insertion order")
        void compactRemovesAllGaps() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e1 = list.addEntry("b");
            list.addEntry("c");
            var e2 = list.addEntry("d");
            list.addEntry("e");
            e1.remove();
            e2.remove();

            list.compact();

            assertThat(list).containsExactly("a", "c", "e");
            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(1)).isEqualTo("c");
            assertThat(list.get(2)).isEqualTo("e");
        }

        @Test
        @DisplayName("compact() on already-compact list is a no-op")
        void compactNoOpWhenAlreadyCompact() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            list.addEntry("b");
            list.addEntry("c");

            list.compact();

            assertThat(list).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("compact() on empty list is a no-op")
        void compactEmptyList() {
            var list = new ElementAwareArrayList<String>();

            assertThatCode(list::compact).doesNotThrowAnyException();
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("get() resolves indexes before and after a gap")
        void getAroundAGap() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            list.addEntry("b");
            var e2 = list.addEntry("c");
            list.addEntry("d");
            e2.remove(); // A gap at slot 2.

            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(1)).isEqualTo("b");
            assertThat(list.get(2)).isEqualTo("d");
        }

        @Test
        @DisplayName("get() after removals before and after an earlier read")
        void getAfterInterleavedRemovals() {
            var list = new ElementAwareArrayList<String>();
            var e0 = list.addEntry("a");
            list.addEntry("b");
            var e2 = list.addEntry("c");
            list.addEntry("d");

            e2.remove();
            assertThat(list.get(2)).isEqualTo("d");
            e0.remove();

            assertThat(list.get(0)).isEqualTo("b");
            assertThat(list.get(1)).isEqualTo("d");
            assertThat(list).containsExactly("b", "d");
        }

        @Execution(ExecutionMode.CONCURRENT)
        @ParameterizedTest
        @MethodSource("randomSeeds")
        @DisplayName("randomized stress test: EAAL matches ArrayList reference under mixed add/remove/get/compact")
        void stressTestAgainstReferenceArrayList(long seed) {
            var random = new Random(seed);
            var reference = new ArrayList<String>();
            var list = new ElementAwareArrayList<String>();
            var liveEntries = new ArrayList<ElementAwareArrayList<String>.Entry>();
            var counter = new AtomicInteger(0);

            for (var i = 0; i < 10_000; i++) {
                var op = random.nextInt(reference.isEmpty() ? 2 : 5);
                switch (op) {
                    case 0, 1 -> { // addEntry (weighted so the list doesn't starve)
                        var element = "e" + counter.getAndIncrement();
                        reference.add(element);
                        liveEntries.add(list.addEntry(element));
                    }
                    case 2 -> { // remove(Entry)
                        var idx = random.nextInt(liveEntries.size());
                        var entry = liveEntries.remove(idx);
                        reference.remove(entry.element());
                        entry.remove();
                    }
                    case 3 -> { // get(int), around any gaps
                        var idx = random.nextInt(reference.size());
                        assertThat(list.get(idx)).isEqualTo(reference.get(idx));
                    }
                    case 4 -> { // compact() then full equality check
                        list.compact();
                        assertThat(list).containsExactlyElementsOf(reference);
                    }
                    default -> {
                        throw new IllegalStateException("Unexpected operation code: %d".formatted(op));
                    }
                }
                assertThat(list).hasSameSizeAs(reference);
            }
            assertThat(list).containsExactlyElementsOf(reference);
        }

        static Stream<Arguments> randomSeeds() {
            var random = new Random(0xCAFEBABEL);
            return random.longs(10)
                    .mapToObj(Arguments::of);
        }

    }

    @Nested
    @DisplayName("Physical slot access")
    class SlotAccessTests {

        @Test
        @DisplayName("slotCount covers gaps, size does not")
        void slotCountCoversGaps() {
            var list = new ElementAwareArrayList<String>();
            assertThat(list.slotCount()).isZero();

            var entryList = fill(list, 20);
            assertThat(list.slotCount()).isEqualTo(20);

            entryList.get(3).remove();
            assertThat(list).hasSize(19);
            assertThat(list.slotCount()).isEqualTo(20); // The gap still occupies its slot.
        }

        @Test
        @DisplayName("entryAt returns null for a gap, but a live entry for a stored null element")
        void entryAtDistinguishesGapFromNullElement() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry(null); // A null element, which is legal.
            var removed = list.addEntry("b");
            list.addEntry("c");
            list.addEntry("d");
            list.addEntry("e");

            removed.remove();

            assertThat(list.entryAt(1)).isNull(); // A gap.
            var nullElementEntry = list.entryAt(0);
            assertThat(nullElementEntry).isNotNull(); // Not a gap...
            assertThat(nullElementEntry.element()).isNull(); // ...but its element is null.
        }

        @Test
        @DisplayName("entryAt rejects slots outside the slot space")
        void entryAtRejectsOutOfRangeSlots() {
            var list = new ElementAwareArrayList<String>();
            fill(list, 4);

            assertThatExceptionOfType(IndexOutOfBoundsException.class)
                    .isThrownBy(() -> list.entryAt(4))
                    .withMessageContaining("slotCount");
            assertThatExceptionOfType(IndexOutOfBoundsException.class)
                    .isThrownBy(() -> list.entryAt(-1))
                    .withMessageContaining("slotCount");
        }

        @Test
        @DisplayName("entryAt does not compact, so slots stay stable across reads")
        void entryAtDoesNotCompact() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 20);
            entryList.get(1).remove();

            for (var slot = 0; slot < list.slotCount(); slot++) {
                list.entryAt(slot);
            }

            assertThat(list.slotCount()).isEqualTo(20);
            assertThat(entryList.get(19).toString()).contains("@19");
        }

    }

    @Nested
    @DisplayName("Growth guard")
    class GrowthGuardTests {

        @Test
        @DisplayName("Interleaved removals and appends keep the slot space within 2.5x the size")
        void slotSpaceStaysBounded() {
            var list = new ElementAwareArrayList<String>();
            var entryList = new ArrayList<>(fill(list, 2000));
            var random = new Random(0);
            var nextElement = 2000;

            for (var round = 0; round < 20_000; round++) {
                var victim = entryList.remove(random.nextInt(entryList.size()));
                victim.remove();
                entryList.add(list.addEntry("e" + nextElement++));
                // Without the compaction in addEntry(), the slot space would grow without bound.
                assertThat(list.slotCount() * 2).isLessThanOrEqualTo(list.size() * 5);
            }

            assertThat(list).hasSize(2000);
            assertThat(copyUsingForEach(list)).hasSize(2000);
        }

        @Test
        @DisplayName("Removals past a quarter of the size leave every other entry in its slot")
        void removeNeverCompacts() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 10);

            for (var i = 0; i < 4; i++) {
                entryList.get(i * 2).remove();
            }

            assertThat(list.slotCount()).isEqualTo(10);
            for (var i = 0; i < 5; i++) {
                assertThat(entryList.get(i * 2 + 1).toString()).contains("@" + (i * 2 + 1));
            }
        }

        @Test
        @DisplayName("Appending to a full array with many gaps compacts in place, in insertion order")
        void appendToFullArrayCompacts() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 16); // The backing array is exactly full.
            for (var i = 0; i < 7; i++) {
                entryList.get(i * 2).remove();
            }

            var newEntry = list.addEntry("new");

            assertThat(list.slotCount()).isEqualTo(10);
            for (var i = 0; i < 9; i++) {
                var entry = list.entryAt(i);
                assertThat(entry).isNotNull();
                assertThat(entry.toString()).contains("@" + i);
            }
            assertThat(newEntry).hasToString("new@9");
            assertThat(list).containsExactly("e1", "e3", "e5", "e7", "e9", "e11", "e13", "e14", "e15", "new");
            entryList.get(5).remove();
            assertThat(list).doesNotContain("e5");
        }

        @Test
        @DisplayName("compactIfGappy compacts only once the gaps pass a quarter of the size")
        void compactIfGappyThreshold() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 10);
            entryList.get(1).remove();
            entryList.get(3).remove(); // 2 gaps, 8 elements: not past a quarter.

            assertThat(list.compactIfGappy()).isFalse();
            assertThat(list.slotCount()).isEqualTo(10);

            entryList.get(5).remove(); // 3 gaps, 7 elements.
            assertThat(list.compactIfGappy()).isTrue();
            assertThat(list.slotCount()).isEqualTo(7);
            assertThat(list).containsExactly("e0", "e2", "e4", "e6", "e7", "e8", "e9");
        }

        @Test
        @DisplayName("Iterating a list with gaps does not move any entry")
        void iterationNeverCompacts() {
            var list = new ElementAwareArrayList<String>();
            var entryList = fill(list, 10);
            for (var i = 0; i < 4; i++) {
                entryList.get(i * 2).remove();
            }

            assertThat(list).containsExactly("e1", "e3", "e5", "e7", "e8", "e9"); // Uses iterator().
            var listIterator = list.listIterator(2);
            assertThat(listIterator.next()).isEqualTo("e5");
            assertThat(listIterator.previous()).isEqualTo("e5");
            assertThat(listIterator.previous()).isEqualTo("e3");

            assertThat(list.slotCount()).isEqualTo(10);
            assertThat(entryList.get(9)).hasToString("e9@9");
        }

        @Test
        @DisplayName("Removing everything through the list iterator")
        void listIteratorRemoveAll() {
            var list = new ElementAwareArrayList<String>();
            fill(list, 50);

            var iterator = list.listIterator();
            var seen = new ArrayList<String>();
            while (iterator.hasNext()) {
                seen.add(iterator.next());
                iterator.remove();
            }

            assertThat(seen).hasSize(50).startsWith("e0", "e1").endsWith("e48", "e49");
            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("Interleaved list iterator removal and traversal stays in insertion order")
        void listIteratorPartialRemove() {
            var list = new ElementAwareArrayList<String>();
            fill(list, 40);

            var iterator = list.listIterator();
            var seen = new ArrayList<String>();
            while (iterator.hasNext()) {
                var element = iterator.next();
                seen.add(element);
                if (seen.size() % 2 == 1) {
                    iterator.remove();
                }
            }

            assertThat(seen).hasSize(40);
            assertThat(list).hasSize(20);
            assertThat(copyUsingForEach(list)).containsExactly(
                    "e1", "e3", "e5", "e7", "e9", "e11", "e13", "e15", "e17", "e19",
                    "e21", "e23", "e25", "e27", "e29", "e31", "e33", "e35", "e37", "e39");
        }

    }

    private static <T extends @Nullable Object> List<T> copyUsingForEach(ElementAwareArrayList<T> list) {
        var result = new ArrayList<T>();
        list.forEach(result::add);
        return result;
    }

    /**
     * Appends the elements {@code e0} to {@code e(elementCount - 1)}.
     */
    private static List<ElementAwareArrayList<String>.Entry> fill(ElementAwareArrayList<String> list, int elementCount) {
        var entryList = new ArrayList<ElementAwareArrayList<String>.Entry>(elementCount);
        for (var i = 0; i < elementCount; i++) {
            entryList.add(list.addEntry("e" + i));
        }
        return entryList;
    }

}
