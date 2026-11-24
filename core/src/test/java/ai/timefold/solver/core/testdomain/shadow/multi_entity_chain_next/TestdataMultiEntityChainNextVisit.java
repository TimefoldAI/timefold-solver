package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultiEntityChainNextVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainNextVehicle vehicle;

    @NextElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainNextVisit nextVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "latestStartTimeSupplier")
    Integer latestStartTime;

    public TestdataMultiEntityChainNextVisit() {
    }

    public TestdataMultiEntityChainNextVisit(String code) {
        super(code);
    }

    public TestdataMultiEntityChainNextVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "vehicle.nextStartTime", "nextVisit", "nextVisit.latestStartTime" })
    public Integer latestStartTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        var base = nextVisit != null ? nextVisit.getLatestStartTime() : vehicle.getNextStartTime();
        if (base == null) {
            return null;
        }
        return base - duration;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TestdataMultiEntityChainNextVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainNextVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainNextVisit getNextVisit() {
        return nextVisit;
    }

    public void setNextVisit(TestdataMultiEntityChainNextVisit nextVisit) {
        this.nextVisit = nextVisit;
    }

    public Integer getLatestStartTime() {
        return latestStartTime;
    }

    public void setLatestStartTime(Integer latestStartTime) {
        this.latestStartTime = latestStartTime;
    }
}
