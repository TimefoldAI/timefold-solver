package ai.timefold.solver.core.impl.bavet.common.joiner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.function.QuadFunction;
import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;
import ai.timefold.solver.core.impl.bavet.penta.joiner.DefaultPentaJoiner;
import ai.timefold.solver.core.impl.bavet.quad.joiner.DefaultQuadJoiner;
import ai.timefold.solver.core.impl.bavet.tri.joiner.DefaultTriJoiner;

import org.junit.jupiter.api.Test;

class ComparatorJoinersTest {

    private record Label(String text) {
    }

    private static final Comparator<Label> BY_LENGTH = Comparator.comparingInt(label -> label.text().length());
    private static final Function<Label, Label> IDENTITY = Function.identity();

    @Test
    void lessThanBiWithNonComparableProperty() {
        DefaultBiJoiner<Label, Label> joiner = (DefaultBiJoiner<Label, Label>) Joiners.lessThan(IDENTITY, IDENTITY, BY_LENGTH);

        assertSoftly(softly -> {
            softly.assertThat(joiner.matches(new Label("a"), new Label("bb"))).isTrue();
            softly.assertThat(joiner.matches(new Label("aaa"), new Label("bb"))).isFalse();
            softly.assertThat(joiner.matches(new Label("a"), new Label("b"))).isFalse();
        });
    }

    @Test
    void comparisonTypesUseTheComparator() {
        var left = new Label("aa");
        var right = new Label("b");
        assertSoftly(softly -> {
            softly.assertThat(((DefaultBiJoiner<Label, Label>) Joiners.lessThanOrEqual(IDENTITY, BY_LENGTH))
                    .matches(left, new Label("bb"))).isTrue();
            softly.assertThat(((DefaultBiJoiner<Label, Label>) Joiners.greaterThan(IDENTITY, BY_LENGTH))
                    .matches(left, right)).isTrue();
            softly.assertThat(((DefaultBiJoiner<Label, Label>) Joiners.greaterThanOrEqual(IDENTITY, BY_LENGTH))
                    .matches(left, new Label("b"))).isTrue();
        });
    }

    @Test
    void alternativeOrderingOnComparableProperty() {
        Function<Integer, Integer> integerIdentity = Function.identity();
        var joiner = (DefaultBiJoiner<Integer, Integer>) Joiners.lessThan(integerIdentity, integerIdentity,
                Comparator.reverseOrder());

        assertSoftly(softly -> {
            softly.assertThat(joiner.matches(10, 5)).isTrue();
            softly.assertThat(joiner.matches(5, 10)).isFalse();
        });
    }

    @Test
    void comparatorDefinesEqualityForInclusiveJoiners() {
        Function<String, String> stringIdentity = Function.identity();
        var joiner = (DefaultBiJoiner<String, String>) Joiners.greaterThanOrEqual(stringIdentity, stringIdentity,
                String.CASE_INSENSITIVE_ORDER);

        assertSoftly(softly -> {
            softly.assertThat(joiner.matches("a", "A")).isTrue();
            softly.assertThat(((DefaultBiJoiner<String, String>) Joiners.lessThan(stringIdentity, stringIdentity,
                    String.CASE_INSENSITIVE_ORDER)).matches("a", "A")).isFalse();
        });
    }

    @Test
    void lessThanTriWithComparator() {
        BiFunction<String, String, String> leftMapping = (a, b) -> a + b;
        var joiner = (DefaultTriJoiner<String, String, Label>) Joiners.lessThan(leftMapping, Label::text,
                Comparator.comparingInt(String::length));

        assertThat(joiner.matches("a", "b", new Label("abc"))).isTrue();
        assertThat(joiner.matches("aaa", "bbb", new Label("a"))).isFalse();
    }

    @Test
    void lessThanQuadWithComparator() {
        TriFunction<String, String, String, String> leftMapping = (a, b, c) -> a + b + c;
        var joiner = (DefaultQuadJoiner<String, String, String, Label>) Joiners.lessThan(leftMapping, Label::text,
                Comparator.comparingInt(String::length));

        assertThat(joiner.matches("a", "b", "c", new Label("abcd"))).isTrue();
        assertThat(joiner.matches("aaa", "bbb", "ccc", new Label("a"))).isFalse();
    }

    @Test
    void greaterThanPentaWithComparator() {
        QuadFunction<String, String, String, String, String> leftMapping = (a, b, c, d) -> a + b + c + d;
        var joiner = (DefaultPentaJoiner<String, String, String, String, Label>) Joiners.greaterThan(leftMapping, Label::text,
                Comparator.comparingInt(String::length));

        assertThat(joiner.matches("a", "b", "c", "d", new Label("a"))).isTrue();
        assertThat(joiner.matches("a", "b", "c", "d", new Label("abcd"))).isFalse();
    }

    @Test
    void overlappingWithComparator() {
        record Interval(Label start, Label end) {
        }

        var joiner = (DefaultBiJoiner<Interval, Interval>) Joiners.overlapping(Interval::start, Interval::end, BY_LENGTH);
        var a = new Interval(new Label("a"), new Label("aaa"));
        var b = new Interval(new Label("bb"), new Label("bbbb"));
        var c = new Interval(new Label("a"), new Label("b"));
        var d = new Interval(new Label("ccc"), new Label("dddd"));

        assertSoftly(softly -> {
            softly.assertThat(joiner.matches(a, b)).isTrue();
            softly.assertThat(joiner.matches(c, d)).isFalse();
        });
    }

    @Test
    void comparatorJoinerCombinesWithEqualJoiner() {
        var joiner = (DefaultBiJoiner<Label, Label>) Joiners.lessThan(IDENTITY, BY_LENGTH)
                .and(Joiners.equal(label -> label.text().length() % 2));

        assertSoftly(softly -> {
            softly.assertThat(joiner.matches(new Label("a"), new Label("ccc"))).isTrue();
            softly.assertThat(joiner.matches(new Label("a"), new Label("bb"))).isFalse();
        });
    }
}
