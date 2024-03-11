package ai.timefold.solver.core.impl.heuristic.selector.list;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

/**
 * A supertype for {@link LocationInList} and {@link UnassignedLocation}.
 * <p>
 * {@link PlanningListVariable#allowsUnassignedValues()} introduces {@link UnassignedLocation},
 * and the handling of unassigned values is brittle.
 * These values may leak into code which expects {@link LocationInList} instead.
 * Therefore, we use {@link ElementLocation} and we force calling code to cast to either of the two subtypes.
 * This prevents accidental use of {@link UnassignedLocation} in places where {@link LocationInList} is expected,
 * catching this error as early as possible.
 */
public sealed interface ElementLocation permits LocationInList, UnassignedLocation {

    static LocationInList of(Object entity, int index) {
        return new LocationInList(entity, index);
    }

    static UnassignedLocation unassigned() {
        return UnassignedLocation.INSTANCE;
    }

}
