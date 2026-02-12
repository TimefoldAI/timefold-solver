package ai.timefold.solver.quarkus.it.domain;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataStringLengthShadowEntity implements TestdataStringLengthShadowEntityInterface {

    private String value;

    private List<String> valueList;

    @ShadowVariable(supplierName = "updateLength")
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

    @ShadowSources("value")
    public Integer updateLength() {
        return Objects.requireNonNullElse(value, "").length();
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

    public void setShadowVarWithParam(Integer shadowVarWithParam) {
        this.shadowVarWithParam = shadowVarWithParam;
    }

    public void setShadowVarWithoutParam(Integer shadowVarWithoutParam) {
        this.shadowVarWithoutParam = shadowVarWithoutParam;
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
