package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataWatchedVisitsVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataWatchedVisitsVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataWatchedVisitsVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataWatchedVisitsVisit() {
    }

    public TestdataWatchedVisitsVisit(String code, int duration) {
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

    public TestdataWatchedVisitsVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataWatchedVisitsVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataWatchedVisitsVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataWatchedVisitsVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }
}
