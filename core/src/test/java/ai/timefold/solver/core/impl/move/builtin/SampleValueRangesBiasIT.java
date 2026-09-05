package ai.timefold.solver.core.impl.move.builtin;

import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.timefold.solver.core.impl.domain.valuerange.ListValueRange;
import ai.timefold.solver.core.impl.neighborhood.bias.AbstractBiasIT;
import ai.timefold.solver.core.impl.neighborhood.bias.BiasReport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link SampleValueRanges#pickExactly} is new reservoir-sampling selection code,
 * not a reuse of anything already bias-tested elsewhere in this package:
 * an off-by-one in its {@code random.nextInt(admittedCount) == 0} pick would silently bias every exact-fallback draw a
 * sample-drawing move iterator makes,
 * and only a direct, algorithm-level test on the helper itself proves it is uniform.
 */
class SampleValueRangesBiasIT extends AbstractBiasIT {

    private static final int TRIAL_COUNT = 200_000;

    /**
     * {@code excludedValue} is exercised by {@code MassChangeMoveProvider}
     * whenever a drawn sample happens to be homogeneous;
     * the {@code null} case is the ordinary, no-exclusion draw.
     */
    @MethodSource("pickExactlyArguments")
    @ParameterizedTest
    void pickExactlyIsUniformOverTheIntersection(Integer excludedValue, List<Integer> expectedValueList) {
        var smallest = new ListValueRange<>(List.of(1, 2, 3, 4, 5));
        var other = new ListValueRange<>(List.of(2, 3, 4, 6, 7)); // intersection with smallest: {2, 3, 4}
        var ranges = SampleValueRanges.of(Set.of(smallest, other));

        var root = new Random(0);
        BiasReport.tally("pickExactly, uniform over the intersection, excludedValue %s".formatted(excludedValue),
                TRIAL_COUNT, trial -> ranges.pickExactly(splitFrom(root), excludedValue))
                .expectUniform(expectedValueList)
                .assertWithinSigma(SIGMA_LIMIT);
    }

    private static List<Arguments> pickExactlyArguments() {
        return List.of(
                Arguments.of(null, List.of(2, 3, 4)),
                Arguments.of(3, List.of(2, 4)));
    }

    /**
     * {@code findDestination}'s FilteringIterator-based sampling path (distinct from {@code pickExactly}'s reservoir pass,
     * bias-tested directly above) is exercised here with a high legal fraction,
     * so the bail-out budget - ten times the smallest range's size - is never exhausted in practice:
     * this isolates the sampling path's own predicate and range-selection wiring from the exact fallback.
     * {@code excludedValue} is again the {@code MassChangeMoveProvider} homogeneous-sample path, untested by the {@code null}
     * case.
     */
    @MethodSource("findDestinationArguments")
    @ParameterizedTest
    void findDestinationIsUniformOverTheIntersectionViaSampling(Integer excludedValue, List<Integer> expectedValueList) {
        var smallest = new ListValueRange<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        var other = new ListValueRange<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 100, 200)); // intersection: 1..8
        var ranges = SampleValueRanges.of(Set.of(smallest, other));

        var root = new Random(0);
        BiasReport.tally(
                "findDestination, uniform over the intersection via sampling, excludedValue %s".formatted(excludedValue),
                TRIAL_COUNT, trial -> ranges.findTarget(splitFrom(root), excludedValue))
                .expectUniform(expectedValueList)
                .assertWithinSigma(SIGMA_LIMIT);
    }

    private static List<Arguments> findDestinationArguments() {
        return List.of(
                Arguments.of(null, List.of(1, 2, 3, 4, 5, 6, 7, 8)),
                Arguments.of(5, List.of(1, 2, 3, 4, 6, 7, 8)));
    }

}
