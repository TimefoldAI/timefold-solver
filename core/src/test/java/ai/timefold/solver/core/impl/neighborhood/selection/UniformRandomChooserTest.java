package ai.timefold.solver.core.impl.neighborhood.selection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;

import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class UniformRandomChooserTest {

    @Test
    void refreshKeepsItemListOrder() {
        var deadSet = new HashSet<>(List.of("b"));
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c", "d"), item -> !deadSet.contains(item));

        // Live list after refresh: a, c, d.
        assertThat(chooser.choose(new TestRandom(0))).isEqualTo("a");
        assertThat(chooser.choose(new TestRandom(1))).isEqualTo("c");
        assertThat(chooser.choose(new TestRandom(2))).isEqualTo("d");
    }

    @Test
    void removeMovesLastItemIntoGap() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c", "d"), item -> true);
        assertThat(chooser.isEmpty()).isFalse();

        chooser.remove("b");
        // Live list: a, d, c.
        assertThat(chooser.choose(new TestRandom(1))).isEqualTo("d");
        assertThat(chooser.choose(new TestRandom(2))).isEqualTo("c");
    }

    @Test
    void removeWhileStaleHasNoEffect() {
        var deadSet = new HashSet<String>();
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c"), item -> !deadSet.contains(item));
        chooser.remove("a");
        deadSet.add("b");

        // Live list after refresh: a, c.
        assertThat(chooser.choose(new TestRandom(0))).isEqualTo("a");
        assertThat(chooser.choose(new TestRandom(1))).isEqualTo("c");
    }

    @Test
    void oneLiveItemUsesNoRandom() {
        var chooser = new UniformRandomChooser<>(List.of("a"), item -> true);
        // TestRandom fails when asked for a number it was not given.
        assertThat(chooser.choose(new TestRandom(new int[0]))).isEqualTo("a");
    }

    @Test
    void emptyGivesNull() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b"), item -> true);
        chooser.isEmpty();
        chooser.remove("a");
        chooser.remove("b");

        assertThat(chooser.isEmpty()).isTrue();
        assertThat(chooser.choose(new TestRandom(0))).isNull();
    }

}
