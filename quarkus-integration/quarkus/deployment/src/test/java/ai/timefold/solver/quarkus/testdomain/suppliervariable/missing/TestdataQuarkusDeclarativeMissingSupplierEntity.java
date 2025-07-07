package ai.timefold.solver.quarkus.testdomain.suppliervariable.missing;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataQuarkusDeclarativeMissingSupplierEntity {
    String id;
    @PlanningVariable
    TestdataQuarkusDeclarativeMissingSupplierValue value;

    @ShadowVariable(supplierName = "updateDurationInDays")
    long durationInDays;

    public TestdataQuarkusDeclarativeMissingSupplierEntity(String id, TestdataQuarkusDeclarativeMissingSupplierValue value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TestdataQuarkusDeclarativeMissingSupplierValue getValue() {
        return value;
    }

    public void setValue(TestdataQuarkusDeclarativeMissingSupplierValue value) {
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
