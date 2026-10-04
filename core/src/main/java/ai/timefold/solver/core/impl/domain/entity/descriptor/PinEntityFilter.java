package ai.timefold.solver.core.impl.domain.entity.descriptor;

import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

import org.jspecify.annotations.Nullable;

/**
 * Filters out entities that return true for the {@link PlanningPin} annotated boolean member.
 * <p>
 * When {@code solutionArgumentSetter} is non-null, {@link #refresh(Object, Object)} invokes it with the working solution
 * so the boolean member can be recomputed and then read as a cache.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
record PinEntityFilter<Solution_>(MemberAccessor memberAccessor, @Nullable MemberAccessor solutionArgumentSetter)
        implements
            MovableFilter<Solution_> {

    void refresh(Solution_ solution, Object entity) {
        if (solutionArgumentSetter != null) {
            solutionArgumentSetter.executeGetter(entity, solution);
        }
    }

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
