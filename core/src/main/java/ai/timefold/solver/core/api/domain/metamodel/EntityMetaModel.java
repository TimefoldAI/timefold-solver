package ai.timefold.solver.core.api.domain.metamodel;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

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
public interface EntityMetaModel<Solution_, Entity_> {

    /**
     * Describes the {@link PlanningSolution} that owns this entity.
     *
     * @return never null, the solution meta-model.
     */
    SolutionMetaModel<Solution_> solution();

    /**
     * Returns the most specific class of the entity.
     *
     * @return never null, the entity type.
     */
    Class<Entity_> type();

    /**
     * Returns the variables owned by the entity, both genuine and shadow.
     *
     * @return never null
     */
    List<VariableMetaModel<Solution_, Entity_, ?>> variables();

    /**
     * Returns the genuine variables owned by the entity.
     *
     * @return never null
     */
    default List<VariableMetaModel<Solution_, Entity_, ?>> genuineVariables() {
        return variables().stream()
                .filter(VariableMetaModel::isGenuine)
                .toList();
    }

    /**
     * Returns a variable meta-model for a variable with the given name.
     *
     * @return never null
     * @throws IllegalArgumentException if the variable does not exist on the entity
     */
    @SuppressWarnings("unchecked")
    default <Value_> VariableMetaModel<Solution_, Entity_, Value_> variable(String variableName) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                return (VariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The variableName (" + variableName + ") does not exist in the variables (" + variables() + ").");
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningVariable basic planning variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> BasicVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName) {
        return (BasicVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningListVariable planning list variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> ListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName) {
        return (ListVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * Returns whether the entity has any genuine variables.
     *
     * @return true if the entity has any genuine variables, false otherwise.
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
