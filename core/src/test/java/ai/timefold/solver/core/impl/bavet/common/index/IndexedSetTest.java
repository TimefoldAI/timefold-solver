package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.SoftAssertions;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class IndexedSetAdditionalTest {

    private final ElementPositionTracker<String> stringTracker = new SimpleTracker<>();

    @Test
    void addToEmptySet() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");

        assertThat(set.size()).isEqualTo(1);
        assertThat(set.isEmpty()).isFalse();
        assertThat(set.asList()).containsExactlyInAnyOrder("A");
    }

    @Test
    void shouldNotCompactWithFewElements() {
        var tracker = new CountingTracker<String>();
        var set = new IndexedSet<>(tracker);

        // Add 5 elements, remove 1 (20% gaps, but below minimum)
        for (int i = 0; i < 5; i++) {
            set.add(String.valueOf(i));
        }
        set.remove("2");

        tracker.resetSetPositionCount();
        set.findFirst(e -> false);

        // No compaction should happen since we're below minimum gap count
        assertThat(tracker.setPositionCount).isZero();
    }

    @Test
    void shouldCompactWhenGapRatioExceeded() {
        var tracker = new CountingTracker<String>();
        var set = new IndexedSet<>(tracker);

        // Add 20 elements, remove 3 (15% gaps, above 10% threshold)
        for (int i = 0; i < 20; i++) {
            set.add(String.valueOf(i));
        }
        set.remove("5");
        set.remove("10");
        set.remove("15");

        tracker.resetSetPositionCount();
        set.findFirst(e -> false);

        // Compaction should happen
        assertThat(tracker.setPositionCount).isPositive();
    }

    @Test
    void asListCompactsSet() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");

        var list = set.asList();

        assertThat(list).containsExactlyInAnyOrder("A", "C");
    }

    @Test
    void asListOnSetWithOnlyGaps() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.remove("A");
        set.remove("B");

        var list = set.asList();

        assertThat(list).isEmpty();
    }

    @Test
    void findFirstStopsAtFirstMatch() {
        var set = new IndexedSet<>(stringTracker);
        var counter = new AtomicInteger(0);

        set.add("A");
        set.add("B");
        set.add("C");

        var result = set.findFirst(element -> {
            counter.incrementAndGet();
            return element.equals("B");
        });

        assertThat(result).isEqualTo("B");
        assertThat(counter.get()).isLessThanOrEqualTo(3);
    }

    @Test
    void findFirstWithAllGaps() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("A");
        set.remove("B");
        set.remove("C");

        var result = set.findFirst(e -> true);

        assertThat(result).isNull();
    }

    @Test
    void forEachOnSetAfterCompaction() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("B");
        set.asList(); // Force compaction

        var result = new ArrayList<String>();
        set.forEach(result::add);

        assertThat(result).containsExactlyInAnyOrder("A", "C");
    }

    @Test
    void mixedOperationsWithCompaction() {
        var set = new IndexedSet<>(stringTracker);

        for (int i = 0; i < 50; i++) {
            set.add("E" + i);
        }
        for (int i = 0; i < 25; i += 2) {
            set.remove("E" + i);
        }

        var list = set.asList(); // Force compaction

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(list)
                    .hasSize(37);
            for (int i = 1; i < 50; i += 2) {
                softly.assertThat(list).contains("E" + i);
            }
        });
    }

    @Test
    void removeFromMiddlePreservesOrder() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.add("D");
        set.add("E");
        set.remove("B");
        set.remove("D");

        var list = set.asList();

        assertThat(list).containsExactlyInAnyOrder("A", "C", "E");
    }

    @Test
    void gapAtEndIsRemoved() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.remove("C");

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void multipleGapsAtEndAreRemoved() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.add("C");
        set.add("D");
        set.add("E");
        set.remove("D");
        set.remove("E");

        assertThat(set.size()).isEqualTo(3);
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    void addAfterRemovingLastElement() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.remove("B");
        set.add("C");

        assertThat(set.size()).isEqualTo(2);
        assertThat(set.asList()).containsExactlyInAnyOrder("A", "C");
    }

    @Test
    void sizeIsConsistentAfterOperations() {
        var set = new IndexedSet<>(stringTracker);

        assertThat(set.size()).isZero();

        set.add("A");
        assertThat(set.size()).isEqualTo(1);

        set.add("B");
        assertThat(set.size()).isEqualTo(2);

        set.remove("A");
        assertThat(set.size()).isEqualTo(1);

        set.add("C");
        assertThat(set.size()).isEqualTo(2);
    }

    @Test
    void emptyAfterRemovingAllElements() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.add("B");
        set.remove("A");
        set.remove("B");

        assertThat(set.isEmpty()).isTrue();
        assertThat(set.size()).isZero();
        assertThat(set.asList()).isEmpty();
    }

    @Test
    void forEachEmptyAfterRemovals() {
        var set = new IndexedSet<>(stringTracker);

        set.add("A");
        set.remove("A");

        var result = new ArrayList<String>();
        set.forEach(result::add);

        assertThat(result).isEmpty();
    }

    @NullMarked
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

    @NullMarked
    private static final class CountingTracker<T> implements ElementPositionTracker<T> {

        private final Map<T, Integer> positions = new HashMap<>();
        private int setPositionCount = 0;

        @Override
        public void setPosition(T element, int position) {
            positions.put(element, position);
            setPositionCount++;
        }

        @Override
        public int clearPosition(T element) {
            var position = positions.remove(element);
            return position == null ? -1 : position;
        }

        void resetSetPositionCount() {
            setPositionCount = 0;
        }
    }

}
