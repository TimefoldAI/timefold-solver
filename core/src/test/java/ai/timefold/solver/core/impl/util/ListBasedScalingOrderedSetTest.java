package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class ListBasedScalingOrderedSetTest {

    @Test
    void emptySetProperties() {
        var set = new ListBasedScalingOrderedSet<String>();

        assertThat(set).isEmpty();
        assertThat(set).hasSize(0);
        assertThat(set).doesNotContain("test");
    }

    @Test
    void addSingleElement() {
        var set = new ListBasedScalingOrderedSet<String>();

        var changed = set.add("test");

        assertThat(changed).isTrue();
        assertThat(set).hasSize(1);
        assertThat(set).contains("test");
    }

    @Test
    void addDuplicateElement() {
        var set = new ListBasedScalingOrderedSet<String>();

        set.add("test");
        var changed = set.add("test");

        assertThat(changed).isFalse();
        assertThat(set).hasSize(1);
        assertThat(set).containsExactly("test");
    }

    @Test
    void addAllWithNewElements() {
        var set = new ListBasedScalingOrderedSet<String>();

        var changed = set.addAll(Arrays.asList("a", "b", "c"));

        assertThat(changed).isTrue();
        assertThat(set).hasSize(3);
        assertThat(set).containsExactly("a", "b", "c");
    }

    @Test
    void addAllWithDuplicateElements() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var changed = set.addAll(Arrays.asList("b", "c"));

        assertThat(changed).isTrue();
        assertThat(set).hasSize(3);
        assertThat(set).containsExactly("a", "b", "c");
    }

    @Test
    void addAllWithAllDuplicateElements() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var changed = set.addAll(Arrays.asList("a", "b"));

        assertThat(changed).isFalse();
        assertThat(set).hasSize(2);
        assertThat(set).containsExactly("a", "b");
    }

    @Test
    void removeSingleElement() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");

        var removed = set.remove("a");

        assertThat(removed).isTrue();
        assertThat(set).isEmpty();
    }

    @Test
    void removeNonexistentElement() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");

        var removed = set.remove("b");

        assertThat(removed).isFalse();
        assertThat(set).hasSize(1);
        assertThat(set).contains("a");
    }

    @Test
    void clearEmptiesTheSet() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        set.clear();

        assertThat(set).isEmpty();
    }

    @Test
    void toArrayReturnsCorrectArray() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var array = set.toArray();

        assertThat(array).containsExactly("a", "b");
    }

    @Test
    void toArrayWithTypeReturnsCorrectArray() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var array = set.toArray(new String[0]);

        assertThat(array).containsExactly("a", "b");
    }

    @Test
    void iteratorReturnsAllElements() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var iterator = set.iterator();

        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("a");
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo("b");
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void iteratorRemoveThrowsException() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");

        var iterator = set.iterator();
        iterator.next();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(iterator::remove);
    }

    @Test
    void containsWorks() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");

        assertThat(set.contains("a")).isTrue();
        assertThat(set.contains("b")).isFalse();
    }

    @Test
    void containsAllWorks() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        assertThat(set.containsAll(Arrays.asList("a", "b"))).isTrue();
        assertThat(set.containsAll(Arrays.asList("a", "c"))).isFalse();
    }

    @Test
    void retainAllThrowsException() {
        var set = new ListBasedScalingOrderedSet<String>();

        assertThatThrownBy(() -> set.retainAll(List.of("a")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("retainAll()");
    }

    @Test
    void removeAllThrowsException() {
        var set = new ListBasedScalingOrderedSet<String>();

        assertThatThrownBy(() -> set.removeAll(List.of("a")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("removeAll()");
    }

    @Test
    void toStringWorks() {
        var set = new ListBasedScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        assertThat(set.toString()).contains("a", "b");
    }

    @Test
    void scalingFromListToSet() {
        var set = new ListBasedScalingOrderedSet<Integer>();

        // Add elements up to the threshold (16)
        for (var i = 0; i < ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }

        // At this point, it should still be using a list
        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Adding one more should cause it to switch to a set
        set.add(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Verify it still works correctly
        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
        for (var i = 0; i <= ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set.contains(i)).isTrue();
        }
    }

    @Test
    void scalingFromSetToList() {
        var set = new ListBasedScalingOrderedSet<Integer>();

        // Add elements beyond threshold to ensure it's using a set
        for (var i = 0; i <= ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }

        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);

        // Remove elements until we're at threshold
        set.remove(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // At threshold, it should still be a set
        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Remove one more to trigger scaling back to list
        set.remove(0);

        // Verify it still works correctly
        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 1);
        assertThat(set.contains(0)).isFalse();
        for (var i = 1; i < ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set.contains(i)).isTrue();
        }
    }

    @Test
    void addAllCausingScaling() {
        var set = new ListBasedScalingOrderedSet<Integer>();

        // Add some elements but stay below threshold
        for (var i = 0; i < ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 5; i++) {
            set.add(i);
        }

        // Prepare a collection that will push it over threshold when added
        var toAdd = List.of(
                ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 5,
                ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 4,
                ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 3,
                ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 2,
                ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD - 1,
                ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Add the collection, which should trigger scaling
        var changed = set.addAll(toAdd);

        assertThat(changed).isTrue();
        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
        for (var i = 0; i <= ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set.contains(i)).isTrue();
        }
    }

    @Test
    void attemptToRemoveNonExistentElementFromSet() {
        var set = new ListBasedScalingOrderedSet<Integer>();

        // Add enough elements to use a set internally
        for (var i = 0; i <= ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }

        // Try to remove an element that doesn't exist
        var removed = set.remove(999);

        // Verify element wasn't removed and set didn't change state
        assertThat(removed).isFalse();
        assertThat(set).hasSize(ListBasedScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
    }
}