package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;

import org.jspecify.annotations.NullMarked;

/**
 * Represents the meta-model of an entity.
 * Gives access to the entity's variable meta-models.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 *
 * @param <Solution_> The solution type.
 * @param <Entity_> The entity type.
 */
@NullMarked
public sealed interface PlanningEntityMetaModel<Solution_, Entity_>
        permits GenuineEntityMetaModel, ShadowEntityMetaModel {

    /**
     * Describes the {@link PlanningSolution} that owns this entity.
     *
     * @return never null, the solution meta-model.
     */
    PlanningSolutionMetaModel<Solution_> solution();

    /**
     * Returns the most specific class of the entity.
     *
     * @return The entity type.
     */
    Class<Entity_> type();

    /**
     * Returns the variables declared by the entity, both genuine and shadow.
     *
     * @return Variables declared by the entity.
     */
    List<? extends VariableMetaModel<Solution_, Entity_, ?>> variables();

    /**
     * Returns a {@link VariableMetaModel} for a variable with the given name.
     * For {@link GenuineEntityMetaModel genuine entities},
     * use {@link GenuineEntityMetaModel#genuineVariable(String)}
     * or {@link GenuineEntityMetaModel#listVariable(String)} when possible.
     *
     * @return A variable declared by the entity.
     */
    @SuppressWarnings("unchecked")
    default <Value_> VariableMetaModel<Solution_, Entity_, Value_> variable(String variableName) {
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
     * but only succeeds if the variable is of a given type.
     * For {@link GenuineEntityMetaModel genuine entities},
     * use {@link GenuineEntityMetaModel#genuineVariable(String, Class)}
     * or {@link GenuineEntityMetaModel#listVariable(String, Class)} when possible.
     *
     * @return A variable declared by the entity.
     */
    @SuppressWarnings("unchecked")
    default <Value_> VariableMetaModel<Solution_, Entity_, Value_> variable(String variableName, Class<Value_> variableClass) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                if (!variableClass.isAssignableFrom(variableMetaModel.type())) {
                    throw new IllegalArgumentException(
                            "The variableName (%s) exists among variables (%s) but is not of type (%s).".formatted(variableName,
                                    variables(), variableClass.getCanonicalName()));
                }
                return (VariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The variableName (%s) does not exist in the variables (%s).".formatted(variableName, variables()));
    }

    /**
     * Checks whether a variable is present on the entity.
     *
     * @return True if present, false otherwise.
     * @see #variable(String) Method to retrieve the variable's meta-model.
     */
    default boolean hasVariable(String variableName) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether the entity declares any genuine variables.
     *
     * @return true if the entity declares any genuine variables, false otherwise.
     */
    default boolean isGenuine() {
        return this instanceof GenuineEntityMetaModel;
    }

}
