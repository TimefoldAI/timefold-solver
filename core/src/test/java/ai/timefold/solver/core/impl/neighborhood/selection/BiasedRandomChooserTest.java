package ai.timefold.solver.core.impl.neighborhood.selection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashSet;
import java.util.List;

import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class BiasedRandomChooserTest {

    @Test
    void choosesByCumulativeWeight() {
        // Offsets: a [0, 1), b [1, 4), c [4, 10).
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 1, 3, 6 }, item -> true);

        assertThat(chooser.choose(new TestRandom(0.05))).isEqualTo("a");
        assertThat(chooser.choose(new TestRandom(0.15))).isEqualTo("b");
        assertThat(chooser.choose(new TestRandom(0.39))).isEqualTo("b");
        assertThat(chooser.choose(new TestRandom(0.41))).isEqualTo("c");
    }

    @Test
    void neverChoosesZeroWeight() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 0, 1 }, item -> true);

        assertThat(chooser.choose(new TestRandom(0.0))).isEqualTo("b");
        assertThat(chooser.choose(new TestRandom(0.99))).isEqualTo("b");
    }

    @Test
    void liveItemsChangeOnlyWhenStale() {
        var deadSet = new HashSet<String>();
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 1, 1 }, item -> !deadSet.contains(item));
        assertThat(chooser.choose(new TestRandom(0.1))).isEqualTo("a");

        deadSet.add("a");
        assertThat(chooser.choose(new TestRandom(0.1))).isEqualTo("a");

        chooser.markStale();
        assertThat(chooser.choose(new TestRandom(0.1))).isEqualTo("b");
    }

    @Test
    void emptyGivesNull() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 0, 1 }, item -> item.equals("a"));

        assertThat(chooser.isEmpty()).isTrue();
        assertThat(chooser.choose(new TestRandom(0.5))).isNull();
    }

    @Test
    void sizeMismatchFails() {
        var itemList = List.of("a");
        var weightArray = new double[] { 1, 2 };
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BiasedRandomChooser<>(itemList, weightArray, item -> true))
                .withMessageContaining("itemList size (1)")
                .withMessageContaining("weightArray length (2)");
    }

}
