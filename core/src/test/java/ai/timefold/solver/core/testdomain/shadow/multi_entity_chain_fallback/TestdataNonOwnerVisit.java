package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataNonOwnerVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataNonOwnerVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataNonOwnerVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataNonOwnerVisit() {
    }

    public TestdataNonOwnerVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        var base = previousVisit != null ? previousVisit.getEndServiceTime() : (Integer) vehicle.getDepartureTime();
        if (base == null) {
            return null;
        }
        return base + duration;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TestdataNonOwnerVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataNonOwnerVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataNonOwnerVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataNonOwnerVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }
}
