package ai.timefold.solver.quarkus.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataStringLengthShadowEntity implements TestdataStringLengthShadowEntityInterface {

    private String value;

    private List<String> valueList;

    @ShadowVariable(variableListenerClass = StringLengthVariableListener.class,
            sourceEntityClass = TestdataStringLengthShadowEntity.class, sourceVariableName = "value")
    private Integer length;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    @Override
    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }
}
