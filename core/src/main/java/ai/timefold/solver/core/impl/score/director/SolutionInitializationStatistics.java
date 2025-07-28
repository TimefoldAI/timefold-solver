package ai.timefold.solver.core.impl.score.director;

/**
 * @param genuineEntityCount
 * @param shadowEntityCount
 * @param uninitializedEntityCount
 * @param uninitializedVariableCount Zero if unassigned values are allowed.
 * @param unassignedValueCount How many values aren't in any list variable,
 *        assuming unassigned values aren't allowed.
 *        Otherwise zero.
 * @param notInAnyListValueCount How many values aren't in any list variable,
 *        regardless of whether unassigned values are allowed.
 */
public record SolutionInitializationStatistics(int genuineEntityCount, int shadowEntityCount, int uninitializedEntityCount,
        int uninitializedVariableCount, int unassignedValueCount, int notInAnyListValueCount) {

    public int getInitCount() {
        return uninitializedVariableCount + uninitializedEntityCount;
    }

    public boolean isInitialized() {
        return getInitCount() == 0;
    }

}
