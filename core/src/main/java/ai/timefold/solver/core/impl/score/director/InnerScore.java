package ai.timefold.solver.core.impl.score.director;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NullMarked;

/**
 * Carries information on if the {@link PlanningSolution} of this score was fully initialized when it was calculated.
 * This only works for solutions where:
 * <ul>
 * <li>{@link PlanningVariable basic variables} are used,
 * and {@link PlanningVariable#allowsUnassigned() unassigning} is not allowed.</li>
 * <li>{@link PlanningListVariable list variables} are used,
 * and {@link PlanningListVariable#allowsUnassignedValues() unassigned values} are not allowed.</li>
 * </ul>
 *
 * For solutions which do allow unassigning values, {@link #unassignedCount} is always zero.
 */
@NullMarked
public record InnerScore<Score_ extends Score<Score_>>(Score_ raw, int unassignedCount)
        implements
            Comparable<InnerScore<Score_>> {

    public static <Score_ extends Score<Score_>> InnerScore<Score_> fullyAssigned(Score_ score) {
        return new InnerScore<>(score, 0);
    }

    public static <Score_ extends Score<Score_>> InnerScore<Score_> withUnassignedCount(Score_ score, int unassignedCount) {
        return new InnerScore<>(score, unassignedCount);
    }

    public InnerScore {
        Objects.requireNonNull(raw);
        if (unassignedCount < 0) {
            throw new IllegalArgumentException("The unassignedCount (%d) must be >= 0."
                    .formatted(unassignedCount));
        }
    }

    public boolean isFullyAssigned() {
        return unassignedCount == 0;
    }

    @Override
    public int compareTo(InnerScore<Score_> other) {
        var uninitializedCountComparison = Integer.compare(unassignedCount, other.unassignedCount);
        if (uninitializedCountComparison != 0) {
            return -uninitializedCountComparison;
        } else {
            return raw.compareTo(other.raw);
        }
    }

    @Override
    public String toString() {
        return isFullyAssigned() ? raw.toString() : "-%dinit/%s".formatted(unassignedCount, raw);
    }
}
