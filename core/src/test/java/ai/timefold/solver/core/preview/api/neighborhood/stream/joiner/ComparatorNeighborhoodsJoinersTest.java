package ai.timefold.solver.core.preview.api.neighborhood.stream.joiner;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Comparator;
import java.util.function.Function;

import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.neighborhood.stream.joiner.DefaultBiNeighborhoodsJoiner;

import org.junit.jupiter.api.Test;

class ComparatorNeighborhoodsJoinersTest {

    private record Label(String text) {
    }

    private static final Comparator<Label> BY_LENGTH = Comparator.comparingInt(label -> label.text().length());
    private static final Function<Label, Label> IDENTITY = Function.identity();

    @Test
    void lessThanWithComparator() {
        var joiner = (DefaultBiNeighborhoodsJoiner<Label, Label>) NeighborhoodsJoiners.lessThan(IDENTITY, BY_LENGTH);
        var biJoiner = (DefaultBiJoiner<Label, Label>) joiner.toBiJoiner();

        assertSoftly(softly -> {
            softly.assertThat(biJoiner.matches(new Label("a"), new Label("bb"))).isTrue();
            softly.assertThat(biJoiner.matches(new Label("aaa"), new Label("bb"))).isFalse();
        });
    }

    @Test
    void overlappingWithComparator() {
        record Interval(Label start, Label end) {
        }

        var joiner = (DefaultBiNeighborhoodsJoiner<Interval, Interval>) NeighborhoodsJoiners.overlapping(Interval::start,
                Interval::end, BY_LENGTH);
        var biJoiner = (DefaultBiJoiner<Interval, Interval>) joiner.toBiJoiner();
        var a = new Interval(new Label("a"), new Label("aaa"));
        var b = new Interval(new Label("bb"), new Label("bbbb"));
        var c = new Interval(new Label("a"), new Label("b"));
        var d = new Interval(new Label("ccc"), new Label("dddd"));

        assertSoftly(softly -> {
            softly.assertThat(biJoiner.matches(a, b)).isTrue();
            softly.assertThat(biJoiner.matches(c, d)).isFalse();
        });
    }
}
