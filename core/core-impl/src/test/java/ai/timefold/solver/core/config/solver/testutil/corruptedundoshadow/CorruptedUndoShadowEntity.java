package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class CorruptedUndoShadowEntity {
    @PlanningId
    String id = CorruptedUndoShadowEntity.class.getSimpleName();

    @PlanningVariable
    String value;

    @ShadowVariable(sourceVariableName = "value",
            variableListenerClass = CorruptedUndoShadowVariableListener.class)
    String valueClone;

    @Override
    public String toString() {
        return CorruptedUndoShadowEntity.class.getSimpleName();
    }
}
