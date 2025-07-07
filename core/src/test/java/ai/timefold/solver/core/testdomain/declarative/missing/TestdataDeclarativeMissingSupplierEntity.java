package ai.timefold.solver.core.testdomain.declarative.missing;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataDeclarativeMissingSupplierEntity {
    String id;
    @PlanningVariable
    TestdataDeclarativeMissingSupplierValue value;

    @ShadowVariable(supplierName = "updateDurationInDays")
    long durationInDays;

    public TestdataDeclarativeMissingSupplierEntity(String id, TestdataDeclarativeMissingSupplierValue value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataDeclarativeMissingSupplierValue getValue() {
        return value;
    }

    public void setValue(TestdataDeclarativeMissingSupplierValue value) {
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
        return "TestdataDeclarativeMissingSupplierEntity{" +
                "id=" + id +
                ", value=" + value +
                '}';
    }
}
