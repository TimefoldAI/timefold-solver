package ai.timefold.solver.core.api.move.factory;

import ai.timefold.solver.core.api.domain.metamodel.BasicVariableMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.ListVariableMetaModel;

public interface MoveDirector<Solution_> {

    <Entity_, Value_> Value_ changeVariable(BasicVariableMetaModel<Solution_, Entity_> variableMetaModel, Entity_ entity,
            Value_ newValue);

    <Entity_> void moveValue(ListVariableMetaModel<Solution_, Entity_> variableMetaModel, Entity_ sourceEntity, int sourceIndex,
            Entity_ destinationEntity, int destinationIndex);

    <Entity_> void moveValue(ListVariableMetaModel<Solution_, Entity_> variableMetaModel, Entity_ entity, int sourceIndex,
            int destinationIndex);

    void updateShadowVariables();

}
