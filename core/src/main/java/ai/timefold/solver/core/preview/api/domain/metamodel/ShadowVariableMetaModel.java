package ai.timefold.solver.core.preview.api.domain.metamodel;

import ai.timefold.solver.core.api.domain.variable.VariableListener;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link VariableMetaModel} that represents a shadow planning variable.
 * The solver doesn't directly modify a shadow variable;
 * its value is derived from genuine variables
 * (see {@link PlanningVariableMetaModel} and {@link PlanningListVariableMetaModel})
 * using a {@link VariableListener} provided either internally or by the user.
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
public non-sealed interface ShadowVariableMetaModel<Solution_, Entity_, Value_>
        extends VariableMetaModel<Solution_, Entity_, Value_> {

    @Override
    default boolean isList() {
        return false;
    }

    @Override
    default boolean isGenuine() {
        return false;
    }

}
