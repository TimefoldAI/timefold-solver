package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class CorruptedUndoShadowEntity {
    @PlanningId
    String id;

    @PlanningVariable
    CorruptedUndoShadowValue value;

    @ShadowVariable(sourceVariableName = "value",
            variableListenerClass = CorruptedUndoShadowVariableListener.class)
    CorruptedUndoShadowValue valueClone;

    public CorruptedUndoShadowEntity() {
    }

    public CorruptedUndoShadowEntity(String id) {
        this.id = id;
    }

    public CorruptedUndoShadowValue getValue() {
        return value;
    }

    public void setValue(CorruptedUndoShadowValue value) {
        this.value = value;
    }

    public CorruptedUndoShadowValue getValueClone() {
        return valueClone;
    }

    public void setValueClone(CorruptedUndoShadowValue valueClone) {
        this.valueClone = valueClone;
    }

    @Override
    public String toString() {
        return CorruptedUndoShadowEntity.class.getSimpleName();
    }
}
