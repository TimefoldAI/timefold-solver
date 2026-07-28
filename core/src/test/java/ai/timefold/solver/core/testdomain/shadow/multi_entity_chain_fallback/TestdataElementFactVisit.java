package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataElementFactVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataElementFactVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataElementFactVisit previousVisit;

    TestdataElementFactVisit buddy;
    int duration = 1;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataElementFactVisit() {
    }

    public TestdataElementFactVisit(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "vehicle", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        var base = previousVisit != null ? previousVisit.getEndServiceTime() : (Integer) vehicle.getDepartureTime();
        if (base == null) {
            return null;
        }
        return base + duration;
    }

    public TestdataElementFactVisit getBuddy() {
        return buddy;
    }

    public void setBuddy(TestdataElementFactVisit buddy) {
        this.buddy = buddy;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TestdataElementFactVehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(TestdataElementFactVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public TestdataElementFactVisit getPreviousVisit() {
        return previousVisit;
    }

    public void setPreviousVisit(TestdataElementFactVisit previousVisit) {
        this.previousVisit = previousVisit;
    }

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }
}
