package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataFactCycleVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataFactCycleVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataFactCycleVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataFactCycleVisit() {
    }

    public TestdataFactCycleVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "vehicle.startTime", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        var base = previousVisit == null ? vehicle.getStartTime() : previousVisit.getEndServiceTime();
        if (base == null) {
            return null;
        }
        return base + duration;
    }

    public int getDuration() {
        return duration;
    }

    public TestdataFactCycleVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataFactCycleVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataFactCycleVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataFactCycleVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }
}
