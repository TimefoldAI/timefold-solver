package ai.timefold.solver.core.api.domain.common;

import java.util.Objects;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.Nullable;

/**
 * Allows to transfer an entity or fact instance (often from another {@link Thread})
 * to another working solution.
 */
public interface Lookup {

    /**
     * Translates an entity or fact instance (often from another {@link Thread})
     * to another working solution.
     * Useful for {@link Move#rebase(Lookup) move rebasing}
     * and in a {@link ProblemChange} and for multi-threaded solving.
     * <p>
     * Matching uses {@link PlanningId}.
     *
     * @param problemFactOrPlanningEntity The fact or entity to rebase.
     * @return null if problemFactOrPlanningEntity is null
     * @throws IllegalArgumentException if there is no working object for the fact or entity,
     *         if it cannot be looked up,
     *         or if its class is not supported.
     * @throws IllegalStateException if it cannot be looked up
     * @param <T> the object type
     */
    <T> @Nullable T lookUpWorkingObject(@Nullable T problemFactOrPlanningEntity);

    /**
     * As defined by {@link #lookUpWorkingObject(Object)},
     * but does not accept null arguments and cannot return null.
     */
    default <T> T lookUpNonNullWorkingObject(T problemFactOrPlanningEntity) {
        return lookUpWorkingObject(Objects.requireNonNull(problemFactOrPlanningEntity));
    }

}
