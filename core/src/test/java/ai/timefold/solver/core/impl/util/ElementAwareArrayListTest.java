package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
        @DisplayName("addEntry reuses the null slot at lastElementPosition when it is a gap")
        void addEntryReusesGapAtTail() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var entryB = list.addEntry("b");
            var entryC = list.addEntry("c");

            entryB.remove(); // gap at slot 1; physical [a@0, null, c@2]
            entryC.remove(); // c is last element → trim; physical [a@0, null], gapCount=1, lastElementPosition=1

            var entryX = list.addEntry("x"); // reuses slot 1 (null at lastElementPosition)

            assertThat(list).containsExactly("a", "x");
            assertThat(entryX.toString()).contains("@1");
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
        @DisplayName("forEach compacts when tail gaps are encountered")
        void forEachCompactsWithTailGaps() {
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
    @DisplayName("Compaction tests")
    class CompactionTests {

        @Test
        @DisplayName("Access compacts the list when gaps exist")
        void accessCompactsWithGaps() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("first");
            var entry2 = list.addEntry("second");
            list.addEntry("third");

            entry2.remove();

            assertThat(list).hasSize(2);
            assertThat(list.get(0)).isEqualTo("first");
            assertThat(list.get(1)).isEqualTo("third");
        }

        @Test
        @DisplayName("Access returns empty when all elements removed")
        void accessWhenAllRemoved() {
            var list = new ElementAwareArrayList<String>();
            var entry1 = list.addEntry("first");

            entry1.remove();

            assertThat(list).isEmpty();
        }

        @Test
        @DisplayName("Access compacts with tail gaps")
        void accessCompactsWithTailGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("first");
            list.add("second");
            var entry3 = list.addEntry("third");
            var entry4 = list.addEntry("fourth");
            var entry5 = list.addEntry("fifth");

            entry3.remove();
            entry4.remove();
            entry5.remove();

            assertThat(list).hasSize(2);
            assertThat(entry3.isRemoved()).isTrue();
            assertThat(entry4.isRemoved()).isTrue();
            assertThat(entry5.isRemoved()).isTrue();
        }

        @Test
        @DisplayName("addAt with trailing gaps only rotates entry into first suffix gap")
        void addAtAfterTrailingGapOnly() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e3.remove();
            e4.remove();
            // Physical: [a, b, null], gapCount=1, size=2 (d trim reduced lastElementPosition)

            list.add(1, "x"); // partialCompact(0): no prefix gap; slot 1 non-null → rotate b into gap at slot 2
            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "b");
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@2");
        }
    }

    @Nested
    @DisplayName("Partial compaction tests")
    class PartialCompactionTests {

        @Test
        @DisplayName("Gaps before target (not at last slot): positions updated, gapCount unchanged")
        void getGapsBeforeTarget_noTrailingCleanup() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e2.remove();
            // Physical: [a, null, c, d], gapCount=1, lastElementPosition=3, size=3

            list.get(1); // c swaps to slot 1; d non-null at slot 3: gapCount(1) < suffix width(2) → no trailing cleanup
            assertThat(e3.toString()).contains("@1"); // position updated
            assertThat(e4.toString()).contains("@3"); // suffix position unchanged
            assertThat(list).hasSize(3);

            // Suffix entry removal still works via its unchanged raw position
            e4.remove();
            assertThat(copyUsingForEach(list)).containsExactly("a", "c");
        }

        @Test
        @DisplayName("Gaps before target (at last slot): trailing cleanup triggered, gapCount=0")
        void getGapsBeforeTarget_trailingCleanup() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");

            e2.remove();
            // Physical: [a, null, c], gapCount=1, lastElementPosition=2, size=2

            list.get(1); // trailing cleanup: gapCount(1) == lastElementPosition(2) - index(1); [a, c]
            assertThat(e3.toString()).contains("@1");
            assertThat(list).hasSize(2);
            // gapCount=0 now; subsequent access is direct
            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(1)).isEqualTo("c");
        }

        @Test
        @DisplayName("Gaps only after target: target found immediately without any swaps")
        void getGapsOnlyAfterTarget() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");

            e3.remove();
            // Physical: [a, b, null], gapCount=1, size=2

            list.getFirst(); // a found at i=0 immediately; no gaps before → no swaps
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@1"); // unchanged
            assertThat(list).hasSize(2);
        }

        @Test
        @DisplayName("Target is last logical element, trailing gaps only (no prefix gaps): trailing cleanup triggered")
        void getLastElement_trailingGapsWithNoPrefix() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e3.remove();
            e4.remove();
            // Physical: [a, b, null, null], gapCount=2, lastElementPosition=3, size=2

            list.get(1); // trailing cleanup: gapCount(2) == lastElementPosition(3) - index(1); trim trailing nulls
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@1");
            assertThat(list).hasSize(2);
            // Trailing cleanup set gapCount=0: subsequent access is direct.
            assertThat(list.get(0)).isEqualTo("a");
            assertThat(list.get(1)).isEqualTo("b");
        }

        @Test
        @DisplayName("Target is last logical element, gaps before and after: trailing cleanup triggered")
        void getLastElement_trailingGapsWithPrefix() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e1.remove();
            e3.remove();
            e4.remove();
            // Physical: [null, b, null, null], gapCount=3, lastElementPosition=3, size=1

            list.getFirst(); // b moves to slot 0; trailing cleanup: gapCount(3) == lastElementPosition(3) - index(0); [b]
            assertThat(e2.toString()).contains("@0");
            assertThat(list).hasSize(1);
            // Trailing cleanup set gapCount=0: subsequent access is direct.
            assertThat(list.getFirst()).isEqualTo("b");
        }

        @Test
        @DisplayName("Gaps before and after target: only the prefix up to target is compacted")
        void getGapsMixedAroundTarget() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e2.remove();
            e4.remove();
            // Physical: [a, null, c, null, e], gapCount=2, lastElementPosition=4, size=3
            // Negative guard: e is non-null in the suffix, so gapCount(2) < suffix width(3) → condition must NOT fire.

            list.get(1); // c at i=2, gaps=1; swap to slot 1; gapCount(2) != lastElementPosition(4) - index(1) → no cleanup
            assertThat(e3.toString()).contains("@1"); // updated
            assertThat(e5.toString()).contains("@4"); // suffix unchanged
            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("a", "c", "e");
        }

        @Test
        @DisplayName("Entry returned by partial compaction has correct position for O(1) removal")
        void removeEntryReturnedByPartialCompact() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            list.addEntry("d");

            e2.remove();
            // Physical: [a, null, c, d], gapCount=1, size=3

            list.get(1); // trigger partial compact: c moves to position 1
            assertThat(e3.toString()).contains("@1");

            e3.remove(); // uses updated position
            assertThat(e3.isRemoved()).isTrue();
            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("a", "d");
        }

        @Test
        @DisplayName("Suffix entry (position not yet updated) retains valid raw position for removal")
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

            list.get(1); // c moves to slot 1; e5.position=4 unchanged

            e5.remove(); // uses raw position 4, still valid (backing list not shrunk)
            assertThat(e5.isRemoved()).isTrue();
            assertThat(list).hasSize(2);
            assertThat(copyUsingForEach(list)).containsExactly("a", "c");
        }

        @Test
        @DisplayName("Sequential get calls migrate gaps rightward progressively")
        void sequentialGet_gapsProgressMigrate() {
            var list = new ElementAwareArrayList<String>();
            list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e2.remove();
            e4.remove();
            // Physical: [a, null, c, null, e], gapCount=2, size=3

            list.get(1);
            assertThat(e3.toString()).contains("@1");

            // Second call: c already at slot 1, no swap needed
            assertThat(list.get(1)).isEqualTo("c");

            // Third call: e migrates to slot 2; trailing cleanup fires
            assertThat(list.get(2)).isEqualTo("e");
            assertThat(e5.toString()).contains("@2");
            assertThat(list).hasSize(3);
            assertThat(copyUsingForEach(list)).containsExactly("a", "c", "e");
        }

        @Test
        @DisplayName("forEach fully compacts list that was previously partially compacted")
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

            list.get(1); // Partial compact → [a, c, null, null, e]

            assertThat(copyUsingForEach(list)).containsExactly("a", "c", "e");
            // After forEach: fully compacted
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@1");
            assertThat(e5.toString()).contains("@2");
            assertThat(list).hasSize(3);
        }

        @Test
        @DisplayName("addAt compacts prefix then rotates entry into gap; all positions correct")
        void getThenAddAt_fullCompactCorrect() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e2.remove();
            // Physical: [a, null, c, d], gapCount=1, size=3

            list.get(1); // Partial compact → [a, c, null, d]

            list.add(1, "x"); // partialCompact(0): no prefix gap; slot 1 non-null → rotate c into gap at slot 2
            assertThat(list).hasSize(4);
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "c", "d");
            // forEach compacts the list fully; entry positions are dense after the call.
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@2");
            assertThat(e4.toString()).contains("@3");
            // gapCount is 0 after forEach; subsequent get() is direct.
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
        @DisplayName("iterator visits null elements")
        void iterator() {
            var list = new ElementAwareArrayList<@Nullable String>();
            list.addEntry(null);
            list.addEntry("a");
            list.addEntry(null);
            var result = copyUsingForEach(list);
            assertThat(result).containsExactly(null, "a", null);
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
            for (int i = 0; i < 100; i++) {
                entries.add(list.addEntry(i));
            }

            // Remove every other element
            for (int i = 0; i < 100; i += 2) {
                entries.get(i).remove();
            }

            assertThat(list).hasSize(50);

            var result = copyUsingForEach(list);
            assertThat(result).hasSize(50);
            for (int i = 0; i < 50; i++) {
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
        @DisplayName("addAt partially compacts prefix then fills gap")
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
        @DisplayName("addAt fills a gap directly when the target slot is null after prefix compaction")
        void addAtFillsGap() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e2.remove();
            // Physical: [a@0, null, c@2, d@3], gapCount=1

            list.add(1, "x");
            // partialCompact(0): a already at slot 0, no swap; slot 1 is null → gap-fill, no shift.

            assertThat(list).hasSize(4);
            assertThat(e1.toString()).contains("@0");
            assertThat(e3.toString()).contains("@2"); // suffix positions unchanged
            assertThat(e4.toString()).contains("@3");
            assertThat(copyUsingForEach(list)).containsExactly("a", "x", "c", "d");
        }

        @Test
        @DisplayName("addAt fills a gap that prefix compaction created at the target slot")
        void addAtFillsGapAfterPrefixCompact() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");

            e1.remove();
            e3.remove();
            // Physical: [null, b@1, null, d@3], gapCount=2

            list.add(1, "x");
            // partialCompact(0): b moves from slot 1 to slot 0, slot 1 becomes null → gap-fill.

            assertThat(list).hasSize(3);
            assertThat(e2.toString()).contains("@0"); // b moved by prefix compaction
            assertThat(e4.toString()).contains("@3"); // d position unchanged
            assertThat(copyUsingForEach(list)).containsExactly("b", "x", "d");
        }

        @Test
        @DisplayName("addAt rotates entries into the nearest suffix gap when target slot is non-null")
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
            // partialCompact(0): no prefix gap; slot 1 non-null (b) → rotate b→2, c→3 into gap at slot 3.

            assertThat(list).hasSize(5);
            // lastElementPosition unchanged; gapCount consumed.
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@2"); // b rotated to slot 2
            assertThat(e3.toString()).contains("@3"); // c rotated into the former gap slot
            assertThat(e5.toString()).contains("@4"); // e beyond the gap: untouched
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
            // logical size = 2, backing size = 3

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
        @DisplayName("addAt rotate stops at nearest suffix gap, leaving farther gaps untouched")
        void addAtRotatesIntoNearestGapMultipleGaps() {
            var list = new ElementAwareArrayList<String>();
            var e1 = list.addEntry("a");
            var e2 = list.addEntry("b");
            var e3 = list.addEntry("c");
            var e4 = list.addEntry("d");
            var e5 = list.addEntry("e");

            e3.remove();
            e4.remove();
            // Physical: [a@0, b@1, null, null, e@4], gapCount=2

            list.add(1, "x");
            // partialCompact(0): no prefix gap; slot 1 non-null (b) → rotate: b→slot 2 (first null), stop.
            // First gap (slot 2) consumed; second gap at slot 3 untouched; e@4 position unchanged.

            assertThat(list).hasSize(4);
            assertThat(e1.toString()).contains("@0");
            assertThat(e2.toString()).contains("@2"); // b rotated into first gap
            assertThat(e5.toString()).contains("@4"); // e beyond both gaps: untouched
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
        @DisplayName("forward iteration skips null slots")
        void forwardWithGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entryB = list.addEntry("b");
            list.add("c");
            var entryD = list.addEntry("d");
            list.add("e");
            entryB.remove();
            entryD.remove();

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isEqualTo("c");
            assertThat(it.next()).isEqualTo("e");
            assertThat(it.hasNext()).isFalse();
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
            assertThat(list).containsExactly("b", "c");
            assertThat(it.hasPrevious()).isFalse();
            assertThat(it.hasNext()).isTrue();
            assertThat(it.next()).isEqualTo("b");
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
        @DisplayName("external remove(Entry) causes CME on subsequent iterator call")
        void cmeOnExternalModify() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("a");
            list.add("b");

            var it = list.listIterator();
            it.next();
            entry.remove();

            assertThatExceptionOfType(ConcurrentModificationException.class)
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
        @DisplayName("listIterator(index) on gappy list compacts before starting")
        void startAtIndexWithGaps() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entry = list.addEntry("b");
            list.add("c");
            list.add("d");
            entry.remove();

            var it = list.listIterator(1);
            assertThat(it.next()).isEqualTo("c");
            assertThat(it.previous()).isEqualTo("c");
            assertThat(it.previous()).isEqualTo("a");
        }

        @Test
        @DisplayName("forward iteration through gaps then full backward traversal")
        void forwardWithGapsThenBackward() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entryB = list.addEntry("b");
            list.add("c");
            var entryD = list.addEntry("d");
            list.add("e");
            entryB.remove();
            entryD.remove();
            // Physical: [a, null, c, null, e]; gaps never compacted during forward scan.

            var it = list.listIterator();
            assertThat(it.next()).isEqualTo("a");
            assertThat(it.next()).isEqualTo("c");
            assertThat(it.next()).isEqualTo("e");
            assertThat(it.hasNext()).isFalse();

            // previous() must traverse back through the uncompacted gaps.
            assertThat(it.previous()).isEqualTo("e");
            assertThat(it.previous()).isEqualTo("c");
            assertThat(it.previous()).isEqualTo("a");
            assertThat(it.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("backward iteration from end of gappy list (gaps at head and middle)")
        void backwardFromEndWithGaps() {
            var list = new ElementAwareArrayList<String>();
            var entryA = list.addEntry("a");
            list.add("b");
            var entryC = list.addEntry("c");
            list.add("d");
            entryA.remove();
            entryC.remove();
            // Physical: [null, b, null, d], logical: [b, d].

            var it = list.listIterator(list.size());
            assertThat(it.hasPrevious()).isTrue();
            assertThat(it.previous()).isEqualTo("d");
            assertThat(it.previous()).isEqualTo("b");
            assertThat(it.hasPrevious()).isFalse();
        }

        @Test
        @DisplayName("iterator remove on list that already has gaps")
        void removeViaIteratorOnGappyList() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entryB = list.addEntry("b");
            list.add("c");
            list.add("d");
            entryB.remove();
            // Physical: [a, null, c, d], logical: [a, c, d].

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
        @DisplayName("next-remove-next-previous cycle with pre-existing gaps")
        void nextRemoveNextPreviousCycle() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            var entryX = list.addEntry("x");
            list.add("b");
            list.add("c");
            entryX.remove();
            // Physical: [a, null, b, c], logical: [a, b, c].

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
    @DisplayName("Fail-fast behavior tests (implementation-specific)")
    class FailFastBehaviorTests {

        @Test
        @DisplayName("listIterator fail-fast on add (implementation-specific)")
        void cmeOnAdd() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");

            var it = list.listIterator();
            it.next();
            list.add("c");

            assertThatExceptionOfType(ConcurrentModificationException.class)
                    .isThrownBy(it::next);
        }

        @Test
        @DisplayName("listIterator fail-fast on remove(int) (implementation-specific)")
        void cmeOnRemove() {
            var list = new ElementAwareArrayList<String>();
            list.add("a");
            list.add("b");

            var it = list.listIterator();
            it.next();
            list.removeFirst();

            assertThatExceptionOfType(ConcurrentModificationException.class)
                    .isThrownBy(it::next);
        }

        @Test
        @DisplayName("listIterator fail-fast on remove(Entry) (implementation-specific)")
        void cmeOnEntryRemove() {
            var list = new ElementAwareArrayList<String>();
            var entry = list.addEntry("a");
            list.add("b");

            var it = list.listIterator();
            it.next();
            entry.remove();

            assertThatExceptionOfType(ConcurrentModificationException.class)
                    .isThrownBy(it::next);
        }

    }

    private static <T extends @Nullable Object> List<T> copyUsingForEach(ElementAwareArrayList<T> list) {
        var result = new ArrayList<T>();
        list.forEach(result::add);
        return result;
    }

}
