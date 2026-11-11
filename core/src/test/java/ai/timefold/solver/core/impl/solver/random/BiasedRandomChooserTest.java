package ai.timefold.solver.core.impl.solver.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class BiasedRandomChooserTest {

    @Test
    void choosesByCumulativeWeight() {
        // Offsets: a [0, 1), b [1, 4), c [4, 10).
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 1, 3, 6 },
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
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 0, Double.MIN_VALUE },
                new TestRandom(0.75));
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void neverChoosesZeroWeight() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 0, 1, 0 },
                new TestRandom(0.0, 0.99));

        assertThat(chooser.choose()).isEqualTo("b");
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void removeChosenShiftsTheOffsetsOfLaterItems() {
        // Offsets: a [0, 1), b [1, 2), c [2, 3).
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 1, 1, 1 },
                new TestRandom(0.5, 0.75));
        assertThat(chooser.choose()).isEqualTo("b");

        chooser.removeChosen();
        // Offsets: a [0, 1), c [1, 2).
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void resetMakesRemovedItemsLiveAgain() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 1, 1 },
                new TestRandom(0.1, 0.1, 0.1));
        assertThat(chooser.choose()).isEqualTo("a");
        chooser.removeChosen();
        assertThat(chooser.choose()).isEqualTo("b");
        chooser.removeChosen();
        assertThat(chooser.isEmpty()).isTrue();

        chooser.reset();
        assertThat(chooser.choose()).isEqualTo("a");
    }

    @Test
    void livePredicateLeavesOutItemsWithoutRandomUse() {
        // TestRandom fails when asked for a number it was not given.
        var chooser = new BiasedRandomChooser<>(List.of("a", "b", "c"), new double[] { 1, 1, 1 },
                item -> !item.equals("b"), new TestRandom(0.75));
        // Offsets: a [0, 1), c [1, 2).
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void livePredicateTestedOncePerItem() {
        var testedList = new ArrayList<String>();
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 1, 1 }, item -> {
            testedList.add(item);
            return true;
        }, new TestRandom(0.1, 0.1));
        assertThat(chooser.choose()).isEqualTo("a");
        chooser.removeChosen();
        assertThat(chooser.choose()).isEqualTo("b");

        assertThat(testedList).containsExactly("a", "b");
    }

    @Test
    void removeChosenWithoutChoiceFails() {
        var chooser = new BiasedRandomChooser<>(List.of("a"), new double[] { 1 }, new TestRandom(0.5));
        assertThatIllegalStateException().isThrownBy(chooser::removeChosen);
    }

    @Test
    void emptyGivesNull() {
        var chooser = new BiasedRandomChooser<>(List.of("a", "b"), new double[] { 0, 0 }, new TestRandom(0.5));

        assertThat(chooser.isEmpty()).isTrue();
        assertThat(chooser.choose()).isNull();
    }

    @Test
    void sizeMismatchFails() {
        var itemList = List.of("a");
        var weightArray = new double[] { 1, 2 };
        var random = new TestRandom(0.5);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BiasedRandomChooser<>(itemList, weightArray, random))
                .withMessageContaining("itemList size (1)")
                .withMessageContaining("weightArray length (2)");
    }

}
