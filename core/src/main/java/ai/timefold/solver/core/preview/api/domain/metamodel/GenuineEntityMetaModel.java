package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.List;

import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

import org.jspecify.annotations.NullMarked;

/**
 * Represents the meta-model of a genuine planning entity, an entity that has at least one genuine planning variable,
 * and may also have shadow variables.
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
public non-sealed interface GenuineEntityMetaModel<Solution_, Entity_>
        extends PlanningEntityMetaModel<Solution_, Entity_> {

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
     */
    <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable();

    /**
     * Returns a {@link PlanningVariableMetaModel} for a variable with the given name.
     *
     * @return A genuine variable declared by the entity.
     */
    <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable(String variableName);

    /**
     * Returns a {@link PlanningVariableMetaModel} for a variable with the given name.
     *
     * @return A genuine variable declared by the entity.
     */
    <Value_> GenuineVariableMetaModel<Solution_, Entity_, Value_> genuineVariable(String variableName,
            Class<Value_> variableClass);

    /**
     * As defined by {@link #genuineVariable()},
     * but only succeeds if the variable is a {@link PlanningVariable basic planning variable}.
     */
    <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable();

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningVariable basic planning variable}.
     */
    <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName);

    /**
     * As defined by {@link #basicVariable(String)},
     * but only succeeds if the variable is of a given type.
     */
    <Value_> PlanningVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName,
            Class<Value_> variableClass);

    /**
     * As defined by {@link #genuineVariable()},
     * but only succeeds if the variable is a {@link PlanningListVariable planning list variable}.
     */
    <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable();

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a {@link PlanningListVariable planning list variable}.
     */
    <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName);

    /**
     * As defined by {@link #listVariable(String)},
     * but only succeeds if the variable is of a given type.
     */
    <Value_> PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName,
            Class<Value_> variableClass);

    /**
     * As defined by {@link #variable(String)},
     * but only succeeds if the variable is a shadow variable.
     */
    <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName);

    /**
     * As defined by {@link #shadowVariable(String)},
     * but only succeeds if the variable is of a given type.
     */
    <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName,
            Class<Value_> variableClass);

}
