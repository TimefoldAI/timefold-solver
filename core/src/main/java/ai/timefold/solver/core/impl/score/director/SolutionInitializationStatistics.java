package ai.timefold.solver.core.impl.score.director;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * Basic statistics of the problem computed in a single pass over its entities.
 * It doesn't build the value ranges of the entities;
 * the problem size statistics, which require them, are computed separately and only when requested.
 *
 * @param genuineEntityCount How many entities have at least one genuine planning variable,
 *        including the pinned ones.
 * @param shadowEntityCount How many entities have only shadow variables.
 * @param uninitializedEntityCount How many genuine entities have at least one uninitialized variable.
 * @param uninitializedVariableCount How many basic variables are {@code null} on all genuine entities.
 *        A variable that {@link PlanningVariable#allowsUnassigned() allows unassigned} is always initialized,
 *        and a list variable is always initialized.
 * @param unassignedValueCount How many values of the list variable's value range aren't in any list,
 *        if the list variable doesn't {@link PlanningListVariable#allowsUnassignedValues() allow unassigned values}.
 *        Otherwise zero, without counting the value range.
 * @param assignedListValueCount How many values are in the list variable, the sum of the sizes of all lists.
 *        Zero if there is no list variable.
 */
public record SolutionInitializationStatistics(int genuineEntityCount, int shadowEntityCount, int uninitializedEntityCount,
        int uninitializedVariableCount, int unassignedValueCount, int assignedListValueCount) {

    public int getInitCount() {
        return uninitializedVariableCount + uninitializedEntityCount;
    }

    public boolean isInitialized() {
        return getInitCount() == 0;
    }

}
