package ai.timefold.solver.core.api.domain.metamodel;

import java.util.List;

public interface EntityMetaModel<Solution_, Entity_> {

    SolutionMetaModel<Solution_> solution();

    Class<Entity_> type();

    List<VariableMetaModel<Solution_, Entity_>> variables();

    default VariableMetaModel<Solution_, Entity_> variable(String variableName) {
        for (var variableMetaModel : variables()) {
            if (variableMetaModel.name().equals(variableName)) {
                return variableMetaModel;
            }
        }
        throw new IllegalArgumentException(
                "The variableName (" + variableName + ") does not exist in the variables (" + variables() + ").");
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
