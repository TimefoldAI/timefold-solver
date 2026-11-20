package ai.timefold.solver.core.testdomain.shadow.fact_group_source;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataFactGroupSourceVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataFactGroupSourceVehicle vehicle;

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    public TestdataFactGroupSourceVisit() {
    }

    public TestdataFactGroupSourceVisit(String code) {
        super(code);
    }

    @ShadowSources("vehicle.startTime")
    public Integer startTimeSupplier() {
        return vehicle == null ? null : vehicle.getStartTime();
    }

    public TestdataFactGroupSourceVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataFactGroupSourceVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }
}
