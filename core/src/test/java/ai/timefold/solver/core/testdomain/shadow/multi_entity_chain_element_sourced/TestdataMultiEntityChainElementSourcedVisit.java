package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A visit reading its vehicle's pre-chain start time, so a vehicle caught in a dependency loop
 * takes its whole route down with it.
 */
@PlanningEntity
public class TestdataMultiEntityChainElementSourcedVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainElementSourcedVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainElementSourcedVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataMultiEntityChainElementSourcedVisit() {
    }

    public TestdataMultiEntityChainElementSourcedVisit(String code, int duration) {
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

    public TestdataMultiEntityChainElementSourcedVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainElementSourcedVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainElementSourcedVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataMultiEntityChainElementSourcedVisit previousVisit) {
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

}
