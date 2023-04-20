package ai.timefold.solver.quarkus.it.devui.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataStringLengthShadowEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    @ShadowVariable(variableListenerClass = StringLengthVariableListener.class,
            sourceEntityClass = TestdataStringLengthShadowEntity.class, sourceVariableName = "value")
    private Integer length;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

}
