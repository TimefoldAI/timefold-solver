package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_watched;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultiEntityChainWatchedVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainWatchedVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainWatchedVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataMultiEntityChainWatchedVisit() {
    }

    public TestdataMultiEntityChainWatchedVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        if (previousVisit == null) {
            return vehicle.getDepartureTime() + duration;
        }
        var previousEndServiceTime = previousVisit.getEndServiceTime();
        return previousEndServiceTime == null ? null : previousEndServiceTime + duration;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TestdataMultiEntityChainWatchedVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainWatchedVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainWatchedVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataMultiEntityChainWatchedVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }
}
