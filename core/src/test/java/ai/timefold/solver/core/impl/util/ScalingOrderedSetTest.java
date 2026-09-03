package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class ScalingOrderedSetTest {

    @Test
    void emptySetProperties() {
        var set = new ScalingOrderedSet<String>();

        assertThat(set)
                .doesNotContain("test")
                .isEmpty();
    }

    @Test
    void addSingleElement() {
        var set = new ScalingOrderedSet<String>();

        var changed = set.add("test");

        assertThat(changed).isTrue();
        assertThat(set)
                .hasSize(1)
                .contains("test");
    }

    @Test
    void addDuplicateElement() {
        var set = new ScalingOrderedSet<String>();

        set.add("test");
        var changed = set.add("test");

        assertThat(changed).isFalse();
        assertThat(set)
                .hasSize(1)
                .containsExactly("test");
    }

    @Test
    void addAllWithNewElements() {
        var set = new ScalingOrderedSet<String>();

        var changed = set.addAll(Arrays.asList("a", "b", "c"));

        assertThat(changed).isTrue();
        assertThat(set)
                .hasSize(3)
                .containsExactly("a", "b", "c");
    }

    @Test
    void addAllWithDuplicateElements() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var changed = set.addAll(Arrays.asList("b", "c"));

        assertThat(changed).isTrue();
        assertThat(set)
                .hasSize(3)
                .containsExactly("a", "b", "c");
    }

    @Test
    void addAllWithAllDuplicateElements() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var changed = set.addAll(Arrays.asList("a", "b"));

        assertThat(changed).isFalse();
        assertThat(set)
                .hasSize(2)
                .containsExactly("a", "b");
    }

    @Test
    void removeSingleElement() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");

        var removed = set.remove("a");

        assertThat(removed).isTrue();
        assertThat(set).isEmpty();
    }

    @Test
    void removeNonexistentElement() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");

        var removed = set.remove("b");

        assertThat(removed).isFalse();
        assertThat(set)
                .hasSize(1)
                .contains("a");
    }

    @Test
    void clearEmptiesTheSet() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        set.clear();

        assertThat(set).isEmpty();
    }

    @Test
    void toArrayReturnsCorrectArray() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var array = set.toArray();

        assertThat(array).containsExactly("a", "b");
    }

    @Test
    void toArrayWithTypeReturnsCorrectArray() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var array = set.toArray(new String[0]);

        assertThat(array).containsExactly("a", "b");
    }

    @Test
    void iteratorReturnsAllElements() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        var iterator = set.iterator();

        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo("a");
        assertThat(iterator).hasNext();
        assertThat(iterator.next()).isEqualTo("b");
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void iteratorRemoveThrowsException() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");

        var iterator = set.iterator();
        iterator.next();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(iterator::remove);
    }

    @Test
    void containsWorks() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");

        assertThat(set)
                .contains("a")
                .doesNotContain("b");
    }

    @Test
    void containsAllWorks() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        assertThat(set).containsAll(Arrays.asList("a", "b"));
        assertThat(set.containsAll(Arrays.asList("a", "c"))).isFalse();
    }

    @Test
    void retainAllThrowsException() {
        var set = new ScalingOrderedSet<String>();

        var list = List.of("a");
        assertThatThrownBy(() -> set.retainAll(list))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("retainAll()");
    }

    @Test
    void removeAllThrowsException() {
        var set = new ScalingOrderedSet<String>();

        var list = List.of("a");
        assertThatThrownBy(() -> set.removeAll(list))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("removeAll()");
    }

    @Test
    void toStringWorks() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");
        set.add("b");

        assertThat(set.toString()).contains("a", "b");
    }

    @Test
    void scalingFromListToSet() {
        var set = new ScalingOrderedSet<Integer>();

        // Add elements up to the threshold (16)
        for (var i = 0; i < ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }

        // At this point, it should still be using a list
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Adding one more should cause it to switch to a set
        set.add(ScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Verify it still works correctly
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
        for (var i = 0; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set).contains(i);
        }
    }

    @Test
    void scalingFromSetToList() {
        var set = new ScalingOrderedSet<Integer>();

        // Add elements beyond threshold to ensure it's using a set
        for (var i = 0; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }

        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);

        // Remove elements until we're at threshold
        set.remove(ScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // At threshold, it should still be a set
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Remove one more to trigger scaling back to list
        set.remove(0);

        // Verify it still works correctly
        assertThat(set)
                .hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD - 1)
                .doesNotContain(0);
        for (var i = 1; i < ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set).contains(i);
        }
    }

    @Test
    void addAllCausingScaling() {
        var set = new ScalingOrderedSet<Integer>();

        // Add some elements but stay below threshold
        for (var i = 0; i < ScalingOrderedSet.LIST_SIZE_THRESHOLD - 5; i++) {
            set.add(i);
        }

        // Prepare a collection that will push it over threshold when added
        var toAdd = List.of(
                ScalingOrderedSet.LIST_SIZE_THRESHOLD - 5,
                ScalingOrderedSet.LIST_SIZE_THRESHOLD - 4,
                ScalingOrderedSet.LIST_SIZE_THRESHOLD - 3,
                ScalingOrderedSet.LIST_SIZE_THRESHOLD - 2,
                ScalingOrderedSet.LIST_SIZE_THRESHOLD - 1,
                ScalingOrderedSet.LIST_SIZE_THRESHOLD);

        // Add the collection, which should trigger scaling
        var changed = set.addAll(toAdd);

        assertThat(changed).isTrue();
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
        for (var i = 0; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set).contains(i);
        }
    }

    @Test
    void attemptToRemoveNonExistentElementFromSet() {
        var set = new ScalingOrderedSet<Integer>();

        // Add enough elements to use a set internally
        for (var i = 0; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }

        // Try to remove an element that doesn't exist
        var removed = set.remove(999);

        // Verify element wasn't removed and set didn't change state
        assertThat(removed).isFalse();
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
    }

    @Test
    void equalsAndHashCodeFollowTheSetContract() {
        var set = new ScalingOrderedSet<String>();
        set.addAll(List.of("a", "b", "c"));
        var otherSet = new LinkedHashSet<>(List.of("c", "a", "b"));

        assertThat(set.equals(otherSet)).isTrue();
        assertThat(otherSet.equals(set)).isTrue();
        assertThat(set.hashCode()).isEqualTo(otherSet.hashCode());
    }

    @Test
    void equalsAndHashCodeFollowTheSetContractAboveTheThreshold() {
        var set = new ScalingOrderedSet<Integer>();
        var otherSet = new LinkedHashSet<Integer>();
        for (var i = 0; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
            otherSet.add(ScalingOrderedSet.LIST_SIZE_THRESHOLD - i);
        }

        assertThat(set.equals(otherSet)).isTrue();
        assertThat(otherSet.equals(set)).isTrue();
        assertThat(set.hashCode()).isEqualTo(otherSet.hashCode());
    }

    @Test
    void nullIsALegalElement() {
        var set = new ScalingOrderedSet<String>();

        assertThat(set.add(null)).isTrue();
        assertThat(set.add(null)).isFalse();
        assertThat(set).hasSize(1);
        assertThat(set.contains(null)).isTrue();
        assertThat(set.getFirst()).isNull();

        set.add("a");
        assertThat(set).containsExactly(null, "a");
        assertThat(set.getLast()).isEqualTo("a");
    }

    @Test
    void emptySetIsDistinctFromASetHoldingNull() {
        var emptySet = new ScalingOrderedSet<String>();
        var nullHoldingSet = new ScalingOrderedSet<String>();
        nullHoldingSet.add(null);

        assertThat(emptySet).isEmpty();
        assertThat(emptySet.contains(null)).isFalse();
        assertThat(nullHoldingSet).isNotEmpty().hasSize(1);
        assertThat(nullHoldingSet.contains(null)).isTrue();
        assertThat(emptySet.equals(nullHoldingSet)).isFalse();
    }

    @Test
    void tiersAdvanceOnAdd() {
        var set = new ScalingOrderedSet<Integer>();
        assertThat(set).isEmpty();

        set.add(0);
        assertThat(set).containsExactly(0);

        set.add(1);
        assertThat(set).containsExactly(0, 1);

        for (var i = 2; i < ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD);

        set.add(ScalingOrderedSet.LIST_SIZE_THRESHOLD);
        assertThat(set).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
        for (var i = 0; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            assertThat(set).contains(i);
        }
    }

    @Test
    void aHintAboveTheThresholdStartsInTheSetTier() {
        var expectedSize = ScalingOrderedSet.LIST_SIZE_THRESHOLD + 4;
        var set = new ScalingOrderedSet<Integer>(expectedSize);
        assertThat(set).isEmpty();

        set.add(0);
        assertThat(set).containsExactly(0);
        assertThat(set.getFirst()).isZero();

        for (var i = 1; i < expectedSize; i++) {
            set.add(i);
        }
        assertThat(set).hasSize(expectedSize);
        assertThat(set.getLast()).isEqualTo(expectedSize - 1);
    }

    @Test
    void expectedSizeConstructorRejectsZeroAndNegative() {
        assertThatThrownBy(() -> new ScalingOrderedSet<String>(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The expectedSize (0)");
        assertThatThrownBy(() -> new ScalingOrderedSet<String>(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The expectedSize (-1)");
    }

    @Test
    void getFirstAndGetLastFollowInsertionOrder() {
        var set = new ScalingOrderedSet<Integer>();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(set::getFirst);
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(set::getLast);

        set.add(0);
        assertThat(set.getFirst()).isZero();
        assertThat(set.getLast()).isZero();

        set.add(1);
        assertThat(set.getFirst()).isZero();
        assertThat(set.getLast()).isEqualTo(1);

        for (var i = 2; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }
        assertThat(set.getFirst()).isZero();
        assertThat(set.getLast()).isEqualTo(ScalingOrderedSet.LIST_SIZE_THRESHOLD);
    }

    @Test
    void reversedIteratesInReverseOrder() {
        var set = new ScalingOrderedSet<Integer>();
        assertThat(set.reversed()).isEmpty();

        set.add(0);
        assertThat(set.reversed()).containsExactly(0);

        set.add(1);
        set.add(2);
        assertThat(set.reversed()).containsExactly(2, 1, 0);
        assertThat(set.reversed().getFirst()).isEqualTo(2);
        assertThat(set.reversed().getLast()).isZero();
    }

    @Test
    void reversedIsALiveView() {
        var set = new ScalingOrderedSet<Integer>();
        set.add(0);
        set.add(1);
        var reversedSet = set.reversed();
        assertThat(reversedSet).containsExactly(1, 0);

        // Cross the list-to-set transition; the view must follow.
        for (var i = 2; i <= ScalingOrderedSet.LIST_SIZE_THRESHOLD; i++) {
            set.add(i);
        }
        assertThat(reversedSet).hasSize(ScalingOrderedSet.LIST_SIZE_THRESHOLD + 1);
        assertThat(reversedSet.getFirst()).isEqualTo(ScalingOrderedSet.LIST_SIZE_THRESHOLD);
        assertThat(reversedSet.getLast()).isZero();
    }

    @Test
    void reversedOfReversedIsTheOriginalSet() {
        var set = new ScalingOrderedSet<String>();
        set.addAll(List.of("a", "b"));

        assertThat(set.reversed().reversed()).isSameAs(set);
    }

    @Test
    void reversedIsReadOnly() {
        var set = new ScalingOrderedSet<String>();
        set.addAll(List.of("a", "b"));
        var reversedSet = set.reversed();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> reversedSet.add("c"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> reversedSet.remove("a"));
    }

    @Test
    void reversedIteratorRemoveThrowsException() {
        var set = new ScalingOrderedSet<String>();
        set.addAll(List.of("a", "b"));
        var iterator = set.reversed().iterator();
        iterator.next();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(iterator::remove);
    }

    @Test
    void addFirstAndAddLastThrowException() {
        var set = new ScalingOrderedSet<String>();
        set.add("a");

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.addFirst("b"));
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> set.addLast("b"));
    }

    @Test
    void clearReturnsToEmpty() {
        var set = new ScalingOrderedSet<String>();
        set.addAll(List.of("a", "b", "c"));
        set.clear();

        assertThat(set).isEmpty();
        assertThat(set.contains("a")).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(set::getFirst);

        set.add("z");
        assertThat(set).containsExactly("z");
        assertThat(set.getFirst()).isEqualTo("z");
    }

}
