package ai.timefold.solver.core.api.domain.metamodel;

import java.util.List;

public interface EntityMetaModel<Solution_, Entity_> {

    SolutionMetaModel<Solution_> solution();

    Class<Entity_> type();

    List<VariableMetaModel<Solution_, Entity_, ?>> variables();

    default List<VariableMetaModel<Solution_, Entity_, ?>> genuineVariables() {
        return variables().stream()
                .filter(VariableMetaModel::isGenuine)
                .toList();
    }

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

    @SuppressWarnings("unchecked")
    default <Value_> BasicVariableMetaModel<Solution_, Entity_, Value_> basicVariable(String variableName) {
        return (BasicVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    @SuppressWarnings("unchecked")
    default <Value_> ListVariableMetaModel<Solution_, Entity_, Value_> listVariable(String variableName) {
        return (ListVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    @SuppressWarnings("unchecked")
    default <Value_> ShadowVariableMetaModel<Solution_, Entity_, Value_> shadowVariable(String variableName) {
        return (ShadowVariableMetaModel<Solution_, Entity_, Value_>) variable(variableName);
    }

    default boolean isGenuine() {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.isGenuine()) {
                return true;
            }
        }
        return false;
    }

}
