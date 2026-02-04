package ai.timefold.solver.quarkus.testdomain.usegettersetter;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataQuarkusUseGetterSetterEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;
    private long getterCallCount;
    private long setterCallCount;

    public String getValue() {
        getterCallCount++;
        return value;
    }

    public void setValue(String value) {
        setterCallCount++;
        this.value = value;
    }

    public long getGetterCallCount() {
        return getterCallCount;
    }

    public long getSetterCallCount() {
        return setterCallCount;
    }
}
