package ai.timefold.solver.core.preview.api.domain.metamodel;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * Represents the meta-model of a shadow entity, an entity which only has shadow variables.
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
public non-sealed interface ShadowEntityMetaModel<Solution_, Entity_> extends PlanningEntityMetaModel<Solution_, Entity_> {

    /**
     * Returns the shadow variables declared by the entity.
     *
     * @return Shadow variables declared by the entity.
     */
    @Override
    List<ShadowVariableMetaModel<Solution_, Entity_, ?>> variables();

    /**
     * Returns a {@link ShadowVariableMetaModel} for a variable with the given name.
     *
     * @return A variable declared by the entity.
     */
    @SuppressWarnings("unchecked")
    @Override
    default <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> variable(String variableName) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                return (ShadowVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel;
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
     */
    @SuppressWarnings("unchecked")
    @Override
    default <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> variable(String variableName,
            Class<Value_> variableClass) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                if (!variableClass.isAssignableFrom(variableMetaModel.type())) {
                    throw new IllegalArgumentException(
                            "The variableName (%s) exists among variables (%s) but is not of type (%s).".formatted(variableName,
                                    variables(), variableClass.getCanonicalName()));
                }
                return (ShadowVariableMetaModel<Solution_, Entity_, Value_>) variableMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The variableName (%s) does not exist in the variables (%s).".formatted(variableName, variables()));
    }

}
