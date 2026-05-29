package ai.timefold.solver.core.impl.bavet.common.joiner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.Joiners;
import ai.timefold.solver.core.impl.bavet.bi.joiner.DefaultBiJoiner;

import org.junit.jupiter.api.Test;

/**
 * Verifies the equal-first reorder primitive ({@code AbstractJoiner.equalsFirstOrder} via
 * {@code DefaultBiJoiner.reorderedEqualsFirst()}) the combers apply, including its node-sharing consequence.
 */
class EqualsFirstReorderTest {

    private record Shift(String employee, int start) {
    }

    private final Function<Shift, String> employeeFn = Shift::employee;
    private final Function<Shift, Integer> startFn = Shift::start;

    @Test
    void movesEqualToFrontCarryingMappings() {
        @SuppressWarnings("unchecked")
        var joiner = (DefaultBiJoiner<Shift, Shift>) Joiners.lessThan(startFn).and(Joiners.equal(employeeFn));
        assertThat(joiner.getJoinerType(0)).isEqualTo(JoinerType.LESS_THAN);
        assertThat(joiner.getJoinerType(1)).isEqualTo(JoinerType.EQUAL);

        var reordered = joiner.reorderedEqualsFirst();
        assertThat(reordered.getJoinerType(0)).isEqualTo(JoinerType.EQUAL);
        assertThat(reordered.getJoinerType(1)).isEqualTo(JoinerType.LESS_THAN);
        // The left/right mappings move together with their joiner type.
        assertThat(reordered.getLeftMapping(0)).isSameAs(employeeFn);
        assertThat(reordered.getLeftMapping(1)).isSameAs(startFn);
        assertThat(reordered.getRightMapping(0)).isSameAs(employeeFn);
        assertThat(reordered.getRightMapping(1)).isSameAs(startFn);
    }

    @Test
    void noEqualReturnsSameInstance() {
        @SuppressWarnings("unchecked")
        var lessThan = (DefaultBiJoiner<Shift, Shift>) Joiners.lessThan(startFn);
        assertThat(lessThan.reorderedEqualsFirst()).isSameAs(lessThan);

        @SuppressWarnings("unchecked")
        var twoComparisons =
                (DefaultBiJoiner<Shift, Shift>) Joiners.lessThan(startFn).and(Joiners.greaterThan(startFn));
        assertThat(twoComparisons.reorderedEqualsFirst()).isSameAs(twoComparisons);
    }

    @Test
    void alreadyEqualFirstReturnsSameInstance() {
        @SuppressWarnings("unchecked")
        var joiner = (DefaultBiJoiner<Shift, Shift>) Joiners.equal(employeeFn).and(Joiners.lessThan(startFn));
        assertThat(joiner.reorderedEqualsFirst()).isSameAs(joiner);
    }

    @Test
    void equivalentJoinsInDifferentOrderShareViaEquality() {
        // Same mapping instances, different declared order: unequal as declared, equal after reorder, so node sharing holds.
        @SuppressWarnings("unchecked")
        var equalFirst = (DefaultBiJoiner<Shift, Shift>) Joiners.equal(employeeFn).and(Joiners.lessThan(startFn));
        @SuppressWarnings("unchecked")
        var equalLast = (DefaultBiJoiner<Shift, Shift>) Joiners.lessThan(startFn).and(Joiners.equal(employeeFn));
        assertThat(equalFirst).isNotEqualTo(equalLast);

        assertThat(equalFirst.reorderedEqualsFirst()).isEqualTo(equalLast.reorderedEqualsFirst());
        assertThat(equalFirst.reorderedEqualsFirst().hashCode())
                .isEqualTo(equalLast.reorderedEqualsFirst().hashCode());
    }

}
