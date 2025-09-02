package ai.timefold.solver.spring.boot.autoconfigure.missingsuppliervariable.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;

@PlanningEntity
public class TestdataSpringMissingSupplierVariableEntity {

    private String value1;
    private String value2;
    private String value1AndValue2;

    // TODO: Remove me when supplier is present
    @ShadowVariablesInconsistent
    private boolean isInconsistent;

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

}
