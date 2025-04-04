package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.score.Score;

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
public record InnerScore<Score_ extends Score<Score_>>(Score_ initialized, int unassignedCount)
        implements
            Comparable<InnerScore<Score_>> {

    public static <Score_ extends Score<Score_>> InnerScore<Score_> of(Score_ score) {
        return ofUninitialized(score, 0);
    }

    public static <Score_ extends Score<Score_>> InnerScore<Score_> ofUninitialized(Score_ score, int uninitializedCount) {
        return new InnerScore<>(score, uninitializedCount);
    }

    public InnerScore {
        if (unassignedCount < 0) {
            throw new IllegalArgumentException("The unassignedCount (" + unassignedCount + ") must be >= 0.");
        }
    }

    public boolean isInitialized() {
        return unassignedCount == 0;
    }

    @Override
    public int compareTo(InnerScore<Score_> o) {
        var uninitializedCountComparison = Integer.compare(unassignedCount, o.unassignedCount);
        if (uninitializedCountComparison != 0) {
            return -uninitializedCountComparison;
        } else {
            return initialized.compareTo(o.initialized);
        }
    }

    @Override
    public String toString() {
        return isInitialized() ? initialized.toString() : "-%dinit/%s".formatted(unassignedCount, initialized);
    }
}
