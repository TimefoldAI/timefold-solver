package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

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
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 *
 * @param <Solution_> The solution type.
 * @param <Entity_> The entity type.
 */
@NullMarked
public interface PlanningEntityMetaModel<Solution_, Entity_> {

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
    List<VariableMetaModel<Solution_, Entity_, ?>> variables();

    /**
     * Returns the genuine variables declared by the entity.
     *
     * @return Genuine variables declared by the entity.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    default List<GenuineVariableMetaModel<Solution_, Entity_, ?>> genuineVariables() {
        return (List) variables().stream()
                .filter(v -> v instanceof GenuineVariableMetaModel)
                .map(v -> (GenuineVariableMetaModel<Solution_, Entity_, ?>) v)
                .toList();
    }

    /**
     * Returns a single genuine variable declared by the entity.
     *
     * @param <Value_> The type of the value of the variable.
     * @return The single genuine variable declared by the entity.
     * @throws IllegalStateException if the entity declares multiple genuine variables, or none.
     */
    @SuppressWarnings("unchecked")
    default <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable() {
        var genuineVariables = genuineVariables();
        return switch (genuineVariables.size()) {
            case 0 -> throw new IllegalStateException("The entity class (%s) has no genuine variables."
                    .formatted(type().getCanonicalName()));
            case 1 -> (GenuineVariableMetaModel<Solution_, Entity_, Value_>) genuineVariables.get(0);
            default -> throw new IllegalStateException("The entity class (%s) has multiple genuine variables (%s)."
                    .formatted(type().getCanonicalName(), genuineVariables));
        };
    }

    /**
     * Returns a {@link PlanningVariableMetaModel} for a variable with the given name.
     *
     * @return A genuine variable declared by the entity.
     * @throws IllegalArgumentException if the variable does not exist on the entity, or is not genuine
     */
    @SuppressWarnings("unchecked")
    default <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable(String variableName) {
        var variable = variable(variableName);
        if (!(variable instanceof GenuineVariableMetaModel<Solution_, Entity_, ?> genuineVariable)) {
            throw new IllegalArgumentException(
                    "The variableName (%s) exists among variables (%s) but is not genuine.".formatted(variableName,
                            variables()));
        }
        return (GenuineVariableMetaModel<Solution_, Entity_, Value_>) genuineVariable;
    }

    /**
     * Returns a {@link PlanningVariableMetaModel} for a variable with the given name.
     *
     * @return A genuine variable declared by the entity.
     * @throws IllegalArgumentException if the variable does not exist on the entity, or is not genuine
     * @throws IllegalArgumentException if the variable is not of the given type.
     */
    default <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable(String variableName,
            Class<Value_> variableClass) {
        var variable = variable(variableName, variableClass);
        if (!(variable instanceof GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable)) {
            throw new IllegalArgumentException(
                    "The variableName (%s) exists among variables (%s) but is not genuine.".formatted(variableName,
                            variables()));
        }
        return genuineVariable;
    }

    /**
     * Returns a {@link VariableMetaModel} for a variable with the given name.
     *
     * @return A variable declared by the entity.
     * @throws IllegalArgumentException where {@link #hasVariable(String)} would have returned false.
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
     *
     * @return A variable declared by the entity.
     * @throws IllegalArgumentException where {@link #hasVariable(String)} would have returned false.
     * @throws IllegalArgumentException if the variable is not of the given type.
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
     * @see #variable(String) Method to retrieve the variable's meta-model, or fail if it is not present.
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
     * As defined by {@link #genuineVariable()},
     * but only succeeds if the variable is a {@link PlanningVariable basic planning variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable() {
        return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) genuineVariable();
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningVariable basic planning variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName) {
        return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * As defined by {@link #basicVariable(String)},
     * but only succeeds if the variable is of a given type.
     * 
     * @throws IllegalArgumentException if the variable is not of the given type.
     */
    default <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName,
            Class<Value_> variableClass) {
        return (PlanningVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName, variableClass);
    }

    /**
     * As defined by {@link #genuineVariable()},
     * but only succeeds if the variable is a {@link PlanningListVariable planning list variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable() {
        return (PlanningListVariableMetaModel<Solution_, Entity_, Value_>) genuineVariable();
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningListVariable planning list variable}.
     */
    @SuppressWarnings("unchecked")
    default <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName) {
        return (PlanningListVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * As defined by {@link #listVariable(String)},
     * but only succeeds if the variable is of a given type.
     * 
     * @throws IllegalArgumentException if the variable is not of the given type.
     */
    default <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName,
            Class<Value_> variableClass) {
        return (PlanningListVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName, variableClass);
    }

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a shadow variable.
     */
    @SuppressWarnings("unchecked")
    default <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName) {
        return (ShadowVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    /**
     * As defined by {@link #shadowVariable(String)},
     * but only succeeds if the variable is of a given type.
     * 
     * @throws IllegalArgumentException if the variable is not of the given type.
     */
    default <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName,
            Class<Value_> variableClass) {
        return (ShadowVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName, variableClass);
    }

    /**
     * Returns whether the entity declares any genuine variables.
     *
     * @return true if the entity declares any genuine variables, false otherwise.
     */
    default boolean isGenuine() {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel instanceof GenuineVariableMetaModel) {
                return true;
            }
        }
        return false;
    }

}
