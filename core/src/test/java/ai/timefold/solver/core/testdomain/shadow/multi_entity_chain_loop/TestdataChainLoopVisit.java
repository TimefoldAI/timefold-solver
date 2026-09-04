package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A visit reading its vehicle's pre-chain start time, so a vehicle caught in a dependency loop
 * takes its whole route down with it.
 */
@PlanningEntity
public class TestdataChainLoopVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataChainLoopVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataChainLoopVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    @ShadowVariablesInconsistent
    Boolean inconsistent;

    public TestdataChainLoopVisit() {
    }

    public TestdataChainLoopVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "vehicle.startTime", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        // The base is transiently null while the vehicles' start times settle.
        var base = previousVisit == null ? vehicle.getStartTime() : previousVisit.getEndServiceTime();
        if (base == null) {
            return null;
        }
        return base + duration;
    }

    public TestdataChainLoopVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataChainLoopVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataChainLoopVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataChainLoopVisit previousVisit) {
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

    public Boolean getInconsistent() {
        return inconsistent;
    }

    public void setInconsistent(Boolean inconsistent) {
        this.inconsistent = inconsistent;
    }
}
