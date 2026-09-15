package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataFactChainVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataFactChainVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataFactChainVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    int calledCount = 0;

    public TestdataFactChainVisit() {
    }

    public TestdataFactChainVisit(String code) {
        super(code);
    }

    public TestdataFactChainVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "vehicle.startTime", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        calledCount++;
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

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TestdataFactChainVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataFactChainVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataFactChainVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataFactChainVisit previousVisit) {
        this.previousVisit = previousVisit;
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
