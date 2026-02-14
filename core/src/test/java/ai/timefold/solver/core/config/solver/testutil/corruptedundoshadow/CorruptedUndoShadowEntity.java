package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class CorruptedUndoShadowEntity {
    @PlanningId
    String id;

    @PlanningVariable
    CorruptedUndoShadowValue value;

    @ShadowVariable(supplierName = "updateValueClone")
    CorruptedUndoShadowValue valueClone;

    public CorruptedUndoShadowEntity() {
    }

    public CorruptedUndoShadowEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    @ShadowSources("value")
    public CorruptedUndoShadowValue updateValueClone() {
        if (valueClone == null || !Objects.equals("v1", value.value)) {
            return value;
        }
        return valueClone;
    }

    @Override
    public String toString() {
        return CorruptedUndoShadowEntity.class.getSimpleName();
    }
}
