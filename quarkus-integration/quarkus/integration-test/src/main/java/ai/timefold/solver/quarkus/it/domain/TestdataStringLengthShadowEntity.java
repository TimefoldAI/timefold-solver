package ai.timefold.solver.quarkus.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataStringLengthShadowEntity implements TestdataStringLengthShadowEntityInterface {

    private String value;

    private List<String> valueList;

    @ShadowVariable(variableListenerClass = StringLengthVariableListener.class,
            sourceEntityClass = TestdataStringLengthShadowEntity.class, sourceVariableName = "value")
    private Integer length;

    @ShadowVariable(supplierName = "processShadowVarWithParam")
    private Integer shadowVarWithParam;

    @ShadowVariable(supplierName = "processShadowVarWithoutParam")
    private Integer shadowVarWithoutParam;

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

    public Integer getShadowVarWithParam() {
        return shadowVarWithParam;
    }

    public Integer getShadowVarWithoutParam() {
        return shadowVarWithoutParam;
    }

    @ShadowSources("value")
    public int processShadowVarWithParam(TestdataStringLengthShadowSolution solution) {
        if (solution == null) {
            throw new NullPointerException("solution is null");
        }
        return solution.getDummyShadowValue();
    }

    @ShadowSources("value")
    public int processShadowVarWithoutParam() {
        return -1;
    }
}
