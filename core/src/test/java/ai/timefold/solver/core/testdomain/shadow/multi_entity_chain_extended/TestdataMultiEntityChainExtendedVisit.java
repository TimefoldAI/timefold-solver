package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_extended;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * The class declaring the previous element, and therefore the element class of its vehicle's list.
 */
@PlanningEntity
public class TestdataMultiEntityChainExtendedVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainExtendedVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainExtendedVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataMultiEntityChainExtendedVisit() {
    }

    public TestdataMultiEntityChainExtendedVisit(String code, int duration) {
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

    public TestdataMultiEntityChainExtendedVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataMultiEntityChainExtendedVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataMultiEntityChainExtendedVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataMultiEntityChainExtendedVisit previousVisit) {
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
