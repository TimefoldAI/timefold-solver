package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultiEntityChainVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainVisit previousVisit;

    int duration = 1;
    boolean chainedToPreviousVehicle = true;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    int calledCount = 0;

    public TestdataMultiEntityChainVisit() {
    }

    public TestdataMultiEntityChainVisit(String code) {
        super(code);
    }

    public TestdataMultiEntityChainVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    public TestdataMultiEntityChainVisit(String code, int duration, boolean chainedToPreviousVehicle) {
        this(code, duration);
        this.chainedToPreviousVehicle = chainedToPreviousVehicle;
    }

    @ShadowSources({ "vehicle", "vehicle.previousEndTime", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        calledCount++;
        if (vehicle == null) {
            return null;
        }
        Integer base;
        if (previousVisit == null) {
            // No unboxing: previousEndTime may be null while the vehicles' values converge.
            base = chainedToPreviousVehicle ? vehicle.getPreviousEndTime() : (Integer) vehicle.getDepartureTime();
        } else {
            var previousEnd = previousVisit.getEndServiceTime();
            if (previousEnd == null) {
                return null;
            }
            if (chainedToPreviousVehicle) {
                var vehicleBase = vehicle.getPreviousEndTime();
                if (vehicleBase == null) {
                    return null;
                }
                base = Math.max(previousEnd, vehicleBase);
            } else {
                base = previousEnd;
            }
        }
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

    public boolean isChainedToPreviousVehicle() {
        return chainedToPreviousVehicle;
    }

    public void setChainedToPreviousVehicle(boolean chainedToPreviousVehicle) {
        this.chainedToPreviousVehicle = chainedToPreviousVehicle;
    }

    public TestdataMultiEntityChainVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataMultiEntityChainVisit previousVisit) {
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
