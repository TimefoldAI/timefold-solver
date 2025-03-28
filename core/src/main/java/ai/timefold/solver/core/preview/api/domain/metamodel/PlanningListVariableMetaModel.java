package ai.timefold.solver.core.preview.api.domain.metamodel;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link VariableMetaModel} that represents a @{@link PlanningVariable list planning variable}.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 *
 * @param <Solution_> the solution type
 * @param <Entity_> the entity type
 * @param <Value_> the value type
 */
@NullMarked
public non-sealed interface PlanningListVariableMetaModel<Solution_, Entity_, Value_>
        extends GenuineVariableMetaModel<Solution_, Entity_, Value_> {

    @Override
    default boolean isList() {
        return true;
    }

    /**
     * Returns whether the planning variable allows values not to be assigned to any entity's list variable.
     *
     * @return {@code true} if the planning variable allows unassigned values, {@code false} otherwise.
     */
    boolean allowsUnassignedValues();

}
