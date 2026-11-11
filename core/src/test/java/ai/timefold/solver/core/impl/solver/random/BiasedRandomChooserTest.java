package ai.timefold.solver.core.impl.solver.random;

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
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 1, 3, 6 }, item -> true,
                new TestRandom(0.0, 0.05, 0.1, 0.39, 0.4, 0.99));

        assertThat(chooser.choose()).isEqualTo("a");
        assertThat(chooser.choose()).isEqualTo("a");
        assertThat(chooser.choose()).isEqualTo("b"); // Exactly on the boundary.
        assertThat(chooser.choose()).isEqualTo("b");
        assertThat(chooser.choose()).isEqualTo("c"); // Exactly on the boundary.
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void offsetRoundedUpToTotalChoosesLastItem() {
        // 0.75 * Double.MIN_VALUE rounds up to Double.MIN_VALUE, the total weight.
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 0, Double.MIN_VALUE }, item -> true,
                new TestRandom(0.75));
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void neverChoosesZeroWeight() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 0, 1, 0 }, item -> true,
                new TestRandom(0.0, 0.99));

        assertThat(chooser.choose()).isEqualTo("b");
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void liveItemsChangeOnlyWhenStale() {
        var deadSet = new HashSet<String>();
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 1, 1 }, item -> !deadSet.contains(item),
                new TestRandom(0.1, 0.1, 0.1));
        assertThat(chooser.choose()).isEqualTo("a");

        deadSet.add("a");
        assertThat(chooser.choose()).isEqualTo("a");

        chooser.markStale();
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void removeChosenFindsLiveItemsAgain() {
        var deadSet = new HashSet<String>();
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 1, 1 }, item -> !deadSet.contains(item),
                new TestRandom(0.1, 0.1));
        assertThat(chooser.choose()).isEqualTo("a");

        deadSet.add("a");
        chooser.removeChosen();
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void emptyGivesNull() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 0, 1 }, item -> item.equals("a"),
                new TestRandom(0.5));

        assertThat(chooser.isEmpty()).isTrue();
        assertThat(chooser.choose()).isNull();
    }

    @Test
    void sizeMismatchFails() {
        var itemList = List.of("a");
        var weightArray = new double[] { 1, 2 };
        var random = new TestRandom(0.5);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BiasedRandomChooser<>(itemList, weightArray, item -> true, random))
                .withMessageContaining("itemList size (1)")
                .withMessageContaining("weightArray length (2)");
    }

}
