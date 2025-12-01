package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common;

import static ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.SelectionProbabilityTest.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class DefaultUniqueRandomSequenceTest {

    @Test
    void emptySet() {
        var emptySet = DefaultUniqueRandomSequence.empty();
        assertThat(emptySet.isEmpty()).isTrue();

        var random = new Random(0);
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> emptySet.pick(random));
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> emptySet.remove(random));
    }

    @Test
    void emptySingleton() {
        var emptySet1 = DefaultUniqueRandomSequence.empty();
        var emptySet2 = DefaultUniqueRandomSequence.empty();
        assertThat(emptySet1).isSameAs(emptySet2);
    }

    @Test
    void singleElementSetPickAndRemove() {
        var list = List.of("A");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        assertThat(set.isEmpty()).isFalse();

        var random = new Random(0);
        var element = set.pick(random);
        assertThat(element.value()).isEqualTo("A");
        assertThat(element.index()).isZero();

        var cleared = set.remove(random);
        assertThat(cleared).isEqualTo("A");
        assertThat(set.isEmpty()).isTrue();

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> set.pick(random));
    }

    @Test
    void singleElementSetRemoveByIndex() {
        var list = List.of("A");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        var cleared = set.remove(0);
        assertThat(cleared).isEqualTo("A");
        assertThat(set.isEmpty()).isTrue();

        var random = new Random(0);
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> set.pick(random));
    }

    @Test
    void multipleElementSet() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        assertThat(set.isEmpty()).isFalse();

        var random = new Random(0);
        var element = set.pick(random);
        assertThat(element.value()).isIn(list);
        assertThat(element.index()).isBetween(0, 4);
    }

    @Test
    void pickDoesNotModifySet() {
        var list = List.of("A", "B", "C");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        var random = new Random(0);
        var element1 = set.pick(random);
        var element2 = set.pick(random);
        var element3 = set.pick(random);

        assertThat(set.isEmpty()).isFalse();
        // Elements may be the same since pick doesn't clear
        assertThat(element1.value()).isIn(list);
        assertThat(element2.value()).isIn(list);
        assertThat(element3.value()).isIn(list);
    }

    @Test
    void removeAllElements() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        var random = new Random(0);
        var clearedElements = new HashSet<String>();

        for (int i = 0; i < 5; i++) {
            assertThat(set.isEmpty()).isFalse();
            var cleared = set.remove(random);
            clearedElements.add(cleared);
        }

        assertThat(set.isEmpty()).isTrue();
        assertThat(clearedElements).containsExactlyInAnyOrderElementsOf(list);

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> set.pick(random));
    }

    @Test
    void removeByIndexSequentially() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        assertThat(set.remove(0)).isEqualTo("A");
        assertThat(set.isEmpty()).isFalse();

        assertThat(set.remove(1)).isEqualTo("B");
        assertThat(set.isEmpty()).isFalse();

        assertThat(set.remove(2)).isEqualTo("C");
        assertThat(set.isEmpty()).isFalse();

        assertThat(set.remove(3)).isEqualTo("D");
        assertThat(set.isEmpty()).isFalse();

        assertThat(set.remove(4)).isEqualTo("E");
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void removeByIndexReverseOrder() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        assertThat(set.remove(4)).isEqualTo("E");
        assertThat(set.remove(3)).isEqualTo("D");
        assertThat(set.remove(2)).isEqualTo("C");
        assertThat(set.remove(1)).isEqualTo("B");
        assertThat(set.remove(0)).isEqualTo("A");

        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void removeByIndexRandomOrder() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        assertThat(set.remove(2)).isEqualTo("C");
        assertThat(set.remove(0)).isEqualTo("A");
        assertThat(set.remove(4)).isEqualTo("E");
        assertThat(set.remove(1)).isEqualTo("B");
        assertThat(set.remove(3)).isEqualTo("D");

        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void pickAfterPartialRemove() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(1); // Clear "B"
        set.remove(3); // Clear "D"

        var random = new Random(0);
        var pickedElements = new HashSet<String>();

        // Pick multiple times to verify we never get cleared elements
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            pickedElements.add(element.value());
            assertThat(element.value()).isNotIn("B", "D");
            assertThat(element.value()).isIn("A", "C", "E");
        }

        // Verify we can pick all remaining elements
        assertThat(pickedElements).containsAnyOf("A", "C", "E");
    }

    @Test
    void removeByIndexThenPickRemaining() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(0); // Clear "A"
        set.remove(4); // Clear "E"

        var random = new Random(42);
        var pickedElements = new HashSet<String>();

        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            pickedElements.add(element.value());
        }

        assertThat(pickedElements)
                .doesNotContain("A", "E")
                .containsAnyOf("B", "C", "D");
    }

    @Test
    void mixedPickAndRemove() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        var random = new Random(0);

        var picked1 = set.pick(random);
        assertThat(picked1.value()).isIn(list);

        var cleared1 = set.remove(random);
        assertThat(cleared1).isIn(list);

        var picked2 = set.pick(random);
        assertThat(picked2.value()).isIn(list);

        set.remove(picked2.index());

        assertThat(set.isEmpty()).isFalse();
    }

    @Test
    void randomDistribution() {
        // Test that random selection is reasonably distributed
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        var random = new Random(12345);
        var counts = new int[5];

        for (int i = 0; i < 1000; i++) {
            var element = set.pick(random);
            counts[element.index()]++;
        }

        // Each element should be picked at least once in 1000 tries
        for (int count : counts) {
            assertThat(count).isGreaterThan(0);
        }
    }

    @Test
    void randomDistributionAfterPartialRemove() {
        // Test that random selection remains distributed after clearing some elements
        var list = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        // Clear some elements
        set.remove(1); // B
        set.remove(5); // F
        set.remove(8); // I

        var random = new Random(12345);
        var pickedElements = new HashSet<String>();

        for (int i = 0; i < 100; i++) {
            var element = set.pick(random);
            pickedElements.add(element.value());
        }

        assertThat(pickedElements)
                .doesNotContain("B", "F", "I")
                .containsAnyOf("A", "C", "D", "E", "G", "H", "J");
    }

    @Test
    void removeUntilOneRemaining() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(0);
        set.remove(1);
        set.remove(2);
        set.remove(4);

        assertThat(set.isEmpty()).isFalse();

        var random = new Random(0);
        var element = set.pick(random);
        assertThat(element.value()).isEqualTo("D");
        assertThat(element.index()).isEqualTo(3);

        set.remove(3);
        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void removeLeftmost() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(0); // Clear leftmost

        var random = new Random(0);
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            assertThat(element.value()).isNotEqualTo("A");
        }
    }

    @Test
    void removeRightmost() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(4); // Clear rightmost

        var random = new Random(0);
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            assertThat(element.value()).isNotEqualTo("E");
        }
    }

    @Test
    void removeBothEnds() {
        var list = List.of("A", "B", "C", "D", "E");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(0); // Clear leftmost
        set.remove(4); // Clear rightmost

        var random = new Random(0);
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            assertThat(element.value()).isIn("B", "C", "D");
        }
    }

    @Test
    void removeConsecutiveFromLeft() {
        var list = List.of("A", "B", "C", "D", "E", "F");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(0);
        set.remove(1);
        set.remove(2);

        var random = new Random(0);
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            assertThat(element.value()).isIn("D", "E", "F");
        }
    }

    @Test
    void removeConsecutiveFromRight() {
        var list = List.of("A", "B", "C", "D", "E", "F");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(5);
        set.remove(4);
        set.remove(3);

        var random = new Random(0);
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            assertThat(element.value()).isIn("A", "B", "C");
        }
    }

    @Test
    void removeAlternatingPattern() {
        var list = List.of("A", "B", "C", "D", "E", "F", "G", "H");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        // Clear every other element
        set.remove(0);
        set.remove(2);
        set.remove(4);
        set.remove(6);

        var random = new Random(0);
        for (int i = 0; i < 20; i++) {
            var element = set.pick(random);
            assertThat(element.value()).isIn("B", "D", "F", "H");
            assertThat(element.index()).isIn(1, 3, 5, 7);
        }
    }

    @Test
    void randomAccessElementRecord() {
        var element = new DefaultUniqueRandomSequence.SequenceElement<>("test", 5);
        assertThat(element.value()).isEqualTo("test");
        assertThat(element.index()).isEqualTo(5);
    }

    @Test
    void multipleRandomSeeds() {
        var list = List.of("A", "B", "C", "D", "E");

        var set1 = new DefaultUniqueRandomSequence<>(toEntries(list));
        var random1 = new Random(123);
        var element1 = set1.pick(random1);

        var set2 = new DefaultUniqueRandomSequence<>(toEntries(list));
        var random2 = new Random(123);
        var element2 = set2.pick(random2);

        // Same seed should produce same result
        assertThat(element1).isEqualTo(element2);

        var set3 = new DefaultUniqueRandomSequence<>(toEntries(list));
        var random3 = new Random(456);
        var element3 = set3.pick(random3);

        // Different seed might produce different result (not guaranteed, but likely)
        // Just verify it doesn't crash
        assertThat(element3.value()).isIn(list);
    }

    @Test
    void largeSet() {
        var list = new ArrayList<String>();
        for (int i = 0; i < 1000; i++) {
            list.add("Element" + i);
        }

        var set = new DefaultUniqueRandomSequence<>(toEntries(list));
        var random = new Random(0);

        // Clear half of the elements
        for (int i = 0; i < 500; i++) {
            assertThatNoException().isThrownBy(() -> set.remove(random));
        }

        assertThat(set.isEmpty()).isFalse();

        // Clear the rest
        for (int i = 0; i < 500; i++) {
            set.remove(random);
        }

        assertThat(set.isEmpty()).isTrue();
    }

    @Test
    void removeSameIndexMultipleTimes() {
        var list = List.of("A", "B", "C");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        set.remove(1); // Clear "B"
        Assertions.assertThatThrownBy(() -> set.remove(1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyUniquenessOfClearedElements() {
        var list = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        var set = new DefaultUniqueRandomSequence<>(toEntries(list));

        var random = new Random(99999);
        var clearedElements = new ArrayList<String>();

        while (!set.isEmpty()) {
            clearedElements.add(set.remove(random));
        }

        // All cleared elements should be unique
        assertThat(clearedElements).hasSize(10);
        assertThat(new HashSet<>(clearedElements)).hasSize(10);
        assertThat(clearedElements).containsExactlyInAnyOrderElementsOf(list);
    }

}
