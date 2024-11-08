package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NonNull;

/**
 * Represents the meta-model of an entity.
 * Gives access to the entity's variable meta-models.
 * <p>
 * <strong>This package and all of its contents are part of the Move Streams API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>.
 *
 * @param <Solution_> The solution type.
 * @param <Entity_> The entity type.
 */
public interface PlanningEntityMetaModel<Solution_, Entity_> {

    /**
     * Describes the {@link PlanningSolution} that owns this entity.
     *
     * @return never null, the solution meta-model.
     */
    @NonNull
    PlanningSolutionMetaModel<Solution_> solution();

    /**
     * Returns the most specific class of the entity.
     *
     * @return The entity type.
     */
    @NonNull
    Class<Entity_> type();

    /**
     * Returns the variables declared by the entity, both genuine and shadow.
     *
     * @return Variables declared by the entity.
     */
    @NonNull
    List<VariableMetaModel<Solution_, Entity_, ?>> variables();

    /**
     * Returns the genuine variables declared by the entity.
     *
     * @return Genuine variables declared by the entity.
     */
    default @NonNull List<VariableMetaModel<Solution_, Entity_, ?>> genuineVariables() {
        return variables().stream()
                .filter(VariableMetaModel::isGenuine)
                .toList();
    }

    /**
     * Returns a {@link VariableMetaModel} for a variable with the given name.
     *
     * @return A variable declared by the entity.
     * @throws IllegalArgumentException if the variable does not exist on the entity
     */
    @SuppressWarnings("unchecked")
    default <Value_> @NonNull VariableMetaModel<Solution_, Entity_, Value_> variable(@NonNull String variableName) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                return (VariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The variableName (%s) does not exist in the variables (%s).".formatted(variableName, variables()));
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningVariable basic planning variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> @NonNull PlanningVariableMetaModel<Solution_, Entity_, Value_>
            planningVariable(@NonNull String variableName) {
        return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningListVariable planning list variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> @NonNull PlanningListVariableMetaModel<Solution_, Entity_, Value_>
            planningListVariable(@NonNull String variableName) {
        return (PlanningListVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * Returns whether the entity declares any genuine variables.
     *
     * @return true if the entity declares any genuine variables, false otherwise.
     */
    default boolean isGenuine() {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.isGenuine()) {
                return true;
            }
        }
        return false;
    }

}
