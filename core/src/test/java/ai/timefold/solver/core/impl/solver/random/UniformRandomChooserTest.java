package ai.timefold.solver.core.impl.solver.random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.List;

import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class UniformRandomChooserTest {

    @Test
    void choosesByIndexInItemListOrder() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c"), new TestRandom(0, 1, 2));
        assertThat(chooser.choose()).isEqualTo("a");
        assertThat(chooser.choose()).isEqualTo("b");
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void removeChosenMovesLastItemIntoGap() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c", "d"), new TestRandom(1, 1, 2));
        assertThat(chooser.choose()).isEqualTo("b");

        chooser.removeChosen();
        // Live list: a, d, c.
        assertThat(chooser.choose()).isEqualTo("d");
        assertThat(chooser.choose()).isEqualTo("c");
    }

    @Test
    void resetRestoresAllItemsInItemListOrder() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b", "c"), new TestRandom(0, 1, 1));
        assertThat(chooser.choose()).isEqualTo("a");
        chooser.removeChosen();
        // Live list: c, b.
        assertThat(chooser.choose()).isEqualTo("b");

        chooser.reset();
        // Live list: a, b, c.
        assertThat(chooser.choose()).isEqualTo("b");
    }

    @Test
    void removeChosenWithoutChoiceFails() {
        var chooser = new UniformRandomChooser<>(List.of("a"), new TestRandom(new int[0]));
        assertThatIllegalStateException().isThrownBy(chooser::removeChosen);
        assertThat(chooser.choose()).isEqualTo("a");
        chooser.removeChosen();
        assertThatIllegalStateException().isThrownBy(chooser::removeChosen);
    }

    @Test
    void oneLiveItemUsesNoRandom() {
        // TestRandom fails when asked for a number it was not given.
        var chooser = new UniformRandomChooser<>(List.of("a"), new TestRandom(new int[0]));
        assertThat(chooser.choose()).isEqualTo("a");
    }

    @Test
    void emptyGivesNull() {
        var chooser = new UniformRandomChooser<>(List.of("a", "b"), new TestRandom(0));
        assertThat(chooser.choose()).isEqualTo("a");
        chooser.removeChosen();
        assertThat(chooser.choose()).isEqualTo("b");
        chooser.removeChosen();

        assertThat(chooser.isEmpty()).isTrue();
        assertThat(chooser.choose()).isNull();
    }

}
