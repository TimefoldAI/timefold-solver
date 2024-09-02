package ai.timefold.solver.core.impl.domain.entity.descriptor;

import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

/**
 * Filters out entities that return true for the {@link PlanningPin} annotated boolean member.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
record PinEntityFilter<Solution_>(MemberAccessor memberAccessor) implements MovableFilter<Solution_> {

    @Override
    public boolean test(Solution_ solution, Object entity) {
        var pinned = (Boolean) memberAccessor.executeGetter(entity);
        if (pinned == null) {
            throw new IllegalStateException("The entity (" + entity + ") has a @" + PlanningPin.class.getSimpleName()
                    + " annotated property (" + memberAccessor.getName() + ") that returns null.");
        }
        return !pinned;
    }

    @Override
    public String toString() {
        return "Non-pinned entities only";
    }
}
