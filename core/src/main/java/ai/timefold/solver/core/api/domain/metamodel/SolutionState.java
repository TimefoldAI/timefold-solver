package ai.timefold.solver.core.api.domain.metamodel;

public interface SolutionState<Solution_> {

    <Entity_, Value_> Value_ getValue(BasicVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity);

    <Entity_, Value_> Value_ getValueAtIndex(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Entity_ entity, int index);

    <Entity_, Value_> ElementLocation getPositionOf(ListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Value_ value);

}
