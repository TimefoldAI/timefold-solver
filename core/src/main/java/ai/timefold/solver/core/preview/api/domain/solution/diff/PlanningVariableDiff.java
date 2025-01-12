package ai.timefold.solver.core.preview.api.domain.solution.diff;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A diff between two values of a single planning variable of a single {@link PlanningEntity},
 * Obtain from {@link PlanningEntityDiff}.
 * <p>
 * This interface is not intended to be implemented by users.
 * The default implementation has a default {@code toString()} method that prints a summary of the differences.
 * Do not attempt to parse that string - it is subject to change in the future.
 *
 * @param <Solution_>
 * @param <Entity_>
 * @param <Value_>
 */
@NullMarked
public interface PlanningVariableDiff<Solution_, Entity_, Value_> {

    /**
     * The parent diff between the two entities, where this diff comes from.
     */
    PlanningEntityDiff<Solution_, Entity_> entityDiff();

    /**
     * Describes the variable that this diff is of.
     */
    VariableMetaModel<Solution_, Entity_, Value_> variableMetaModel();

    /**
     * The entity that this diff is about.
     */
    default Entity_ entity() {
        return entityDiff().entity();
    }

    /**
     * The old value of the variable.
     *
     * @return Null if the variable was null.
     */
    @Nullable
    Value_ oldValue();

    /**
     * The new value of the variable.
     *
     * @return Null if the variable is null.
     */
    @Nullable
    Value_ newValue();

}
