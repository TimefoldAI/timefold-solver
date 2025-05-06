package ai.timefold.solver.core.testdomain.declarative.basic;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataBasicVarEntity {
    String id;
    @PlanningVariable
    TestdataBasicVarValue value;

    @ShadowVariable(supplierName = "updateDurationInDays")
    long durationInDays;

    public TestdataBasicVarEntity(String id, TestdataBasicVarValue value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataBasicVarValue getValue() {
        return value;
    }

    public void setValue(TestdataBasicVarValue value) {
        this.value = value;
    }

    @ShadowSources("value")
    public long updateDurationInDays() {
        if (value != null) {
            return value.getDuration().toDays();
        }
        return 0;
    }

    public long getDurationInDays() {
        return durationInDays;
    }

    @Override
    public String toString() {
        return "TestdataBasicVarEntity{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
