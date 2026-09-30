package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultiEntityChainDepartureVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainDepartureVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainDepartureVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    int calledCount = 0;

    public TestdataMultiEntityChainDepartureVisit() {
    }

    public TestdataMultiEntityChainDepartureVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "vehicle.departureTime", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        calledCount++;
        if (vehicle == null) {
            return null;
        }
        var base = previousVisit == null ? vehicle.getDepartureTime() : previousVisit.getEndServiceTime();
        if (base == null) {
            return null;
        }
        return base + duration;
    }

    public TestdataMultiEntityChainDepartureVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainDepartureVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainDepartureVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataMultiEntityChainDepartureVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public int getDuration() {
        return duration;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }

    public int getCalledCount() {
        return calledCount;
    }

    public void reset() {
        calledCount = 0;
    }
}
