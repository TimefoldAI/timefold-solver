package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

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
public class TestdataExtendedVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataExtendedVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataExtendedVisit previousVisit;

    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataExtendedVisit() {
    }

    public TestdataExtendedVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        var base = previousVisit == null ? (Integer) vehicle.getDepartureTime() : previousVisit.getEndServiceTime();
        if (base == null) {
            return null;
        }
        return base + duration;
    }

    public TestdataExtendedVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataExtendedVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataExtendedVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataExtendedVisit previousVisit) {
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
