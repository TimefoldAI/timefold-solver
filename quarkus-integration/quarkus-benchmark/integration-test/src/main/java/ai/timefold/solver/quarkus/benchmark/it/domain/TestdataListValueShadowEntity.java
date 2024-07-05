package ai.timefold.solver.quarkus.benchmark.it.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataListValueShadowEntity {

    private String value;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    private TestdataStringLengthShadowEntity entity;

    @ShadowVariable(variableListenerClass = StringLengthVariableListener.class, sourceVariableName = "entity")
    private Integer length;

    public TestdataListValueShadowEntity() {
    }

    public TestdataListValueShadowEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public TestdataStringLengthShadowEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataStringLengthShadowEntity entity) {
        this.entity = entity;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }
}
