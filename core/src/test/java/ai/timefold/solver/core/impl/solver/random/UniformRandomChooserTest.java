package ai.timefold.solver.core.impl.solver.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;

import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class UniformRandomChooserTest {

    @Test
    void refreshKeepsItemListOrder() {
        var deadSet = new HashSet<>(List.of("b"));
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c", "d"), item -> !deadSet.contains(item),
                new TestRandom(0, 1, 2));
        chooser.markStale();

        // Live list after refresh: a, c, d.
        assertThat(chooser.choose()).isEqualTo("a");
        assertThat(chooser.choose()).isEqualTo("c");
        assertThat(chooser.choose()).isEqualTo("d");
    }

    @Test
    void allItemsStartLiveWithoutPredicate() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b"), item -> {
            throw new IllegalStateException("Not evaluated before markStale().");
        }, new TestRandom(1));
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void removeChosenMovesLastItemIntoGap() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c", "d"), item -> true, new TestRandom(1, 1, 2));
        assertThat(chooser.choose()).isEqualTo("b");

        chooser.removeChosen();
        // Live list: a, d, c.
        assertThat(chooser.choose()).isEqualTo("d");
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void markStaleDoesNotRestoreDeadItems() {
        var deadSet = new HashSet<String>();
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c"), item -> !deadSet.contains(item),
                new TestRandom(0, 0, 1));
        assertThat(chooser.choose()).isEqualTo("a");
        deadSet.add("a");
        chooser.removeChosen();
        chooser.markStale();

        // Live list after refresh: b, c.
        assertThat(chooser.choose()).isEqualTo("b");
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void oneLiveItemUsesNoRandom() {
        // TestRandom fails when asked for a number it was not given.
        var chooser = new UniformRandomChooser<>(List.of("a"), item -> true, new TestRandom(new int[0]));
        assertThat(chooser.choose()).isEqualTo("a");
    }

    @Test
    void emptyGivesNull() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b"), item -> true, new TestRandom(0));
        assertThat(chooser.choose()).isEqualTo("a");
        chooser.removeChosen();
        assertThat(chooser.choose()).isEqualTo("b");
        chooser.removeChosen();

        assertThat(chooser.isEmpty()).isTrue();
        assertThat(chooser.choose()).isNull();
    }

}
