package ai.timefold.solver.core.preview.api.domain.metamodel;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NullMarked;

/**
 * A {@link VariableMetaModel} that represents a @{@link PlanningVariable basic planning variable}.
 *
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
public sealed interface GenuineVariableMetaModel<Solution_, Entity_, Value_>
        extends VariableMetaModel<Solution_, Entity_, Value_>
        permits PlanningVariableMetaModel, PlanningListVariableMetaModel {

    @Override
    default boolean isGenuine() {
        return true;
    }

    default PlanningVariableMetaModel<Solution_, Entity_, Value_> ensurePlanningVariable() {
        if (this instanceof PlanningVariableMetaModel<Solution_, Entity_, Value_> planningVariableMetaModel) {
            return planningVariableMetaModel;
        } else {
            throw new IllegalStateException("Genuine variable (%s) is not @%s."
                    .formatted(this, PlanningVariable.class.getSimpleName()));
        }
    }

    default PlanningListVariableMetaModel<Solution_, Entity_, Value_> ensurePlanningListVariable() {
        if (this instanceof PlanningListVariableMetaModel<Solution_, Entity_, Value_> planningListVariableMetaModel) {
            return planningListVariableMetaModel;
        } else {
            throw new IllegalStateException("Genuine variable (%s) is not @%s."
                    .formatted(this, PlanningListVariable.class.getSimpleName()));
        }
    }

}
