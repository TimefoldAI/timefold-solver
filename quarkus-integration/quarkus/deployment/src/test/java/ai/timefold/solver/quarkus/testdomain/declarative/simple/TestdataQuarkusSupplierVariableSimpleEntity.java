package ai.timefold.solver.quarkus.testdomain.declarative.simple;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;

@PlanningEntity
public class TestdataQuarkusSupplierVariableSimpleEntity {

    private String value1;
    private String value2;
    private String value1AndValue2;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    @ShadowVariable(supplierName = "value1AndValue2Supplier")
    public String getValue1AndValue2() {
        return value1AndValue2;
    }

    public void setValue1AndValue2(String value1AndValue2) {
        this.value1AndValue2 = value1AndValue2;
    }

    @ShadowSources({ "value1", "value2" })
    public String value1AndValue2Supplier() {
        if (value1 == null || value2 == null) {
            return null;
        }
        return value1 + value2;
    }

}
