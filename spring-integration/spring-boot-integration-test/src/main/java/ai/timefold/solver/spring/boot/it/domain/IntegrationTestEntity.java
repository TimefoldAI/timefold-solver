package ai.timefold.solver.spring.boot.it.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

import com.fasterxml.jackson.annotation.JsonIdentityReference;

@PlanningEntity
public class IntegrationTestEntity {
    @PlanningId
    private String id;

    @PlanningVariable(valueRangeProviderRefs = { "valueRange", "valueRangeWithParameter" })
    @JsonIdentityReference(alwaysAsId = true)
    private IntegrationTestValue value;

    private List<IntegrationTestValue> valueList;

    @ShadowVariable(supplierName = "processShadowVarWithParam")
    private Integer shadowVarWithParam;

    @ShadowVariable(supplierName = "processShadowVarWithoutParam")
    private Integer shadowVarWithoutParam;

    public IntegrationTestEntity() {
    }

    public IntegrationTestEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public IntegrationTestValue getValue() {
        return value;
    }

    public void setValue(IntegrationTestValue value) {
        this.value = value;
    }

    @ValueRangeProvider(id = "valueRange")
    public List<IntegrationTestValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<IntegrationTestValue> valueList) {
        this.valueList = valueList;
    }

    @ValueRangeProvider(id = "valueRangeWithParameter")
    public List<IntegrationTestValue> getValueRangeWithParameter(IntegrationTestSolution solution) {
        return solution.getValueList();
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
    public int processShadowVarWithParam(IntegrationTestSolution solution) {
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
