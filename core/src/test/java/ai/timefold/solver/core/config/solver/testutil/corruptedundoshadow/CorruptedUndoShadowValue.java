package ai.timefold.solver.core.config.solver.testutil.corruptedundoshadow;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;

@PlanningEntity
public class CorruptedUndoShadowValue {
    @PlanningId
    String value;

    @InverseRelationShadowVariable(sourceVariableName = "value")
    List<CorruptedUndoShadowEntity> entities;

    public CorruptedUndoShadowValue() {
    }

    public CorruptedUndoShadowValue(String value) {
        this.value = value;
        this.entities = new ArrayList<>();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<CorruptedUndoShadowEntity> getEntities() {
        return entities;
    }

    public void setEntities(
            List<CorruptedUndoShadowEntity> entities) {
        this.entities = entities;
    }

    @Override
    public String toString() {
        return value;
    }
}
