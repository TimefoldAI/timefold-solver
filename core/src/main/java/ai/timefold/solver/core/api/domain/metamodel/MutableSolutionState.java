package ai.timefold.solver.core.api.domain.metamodel;

public interface MutableSolutionState<Solution_> extends SolutionState<Solution_> {

    <Entity_, Value_> void changeVariable(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            Value_ newValue);

    <Entity_, Value_> void moveValue(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ sourceEntity,
            int sourceIndex, Entity_ destinationEntity, int destinationIndex);

    <Entity_, Value_> void moveValue(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity,
            int sourceIndex, int destinationIndex);

    void updateShadowVariables();

}
