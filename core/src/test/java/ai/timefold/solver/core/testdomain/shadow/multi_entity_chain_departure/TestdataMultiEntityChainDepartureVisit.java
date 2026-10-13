package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A visit reading a planning variable of its vehicle, which no edge orders before the chain node.
 */
@PlanningEntity
public class TestdataMultiEntityChainDepartureVisit extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainDepartureVehicle vehicle;

    @PreviousElementShadowVariable(sourceVariableName = "visits")
    TestdataMultiEntityChainDepartureVisit previousVisit;

    @ShadowVariable(supplierName = "endServiceTimeSupplier")
    Integer endServiceTime;

    public TestdataMultiEntityChainDepartureVisit() {
    }

    public TestdataMultiEntityChainDepartureVisit(String code) {
        super(code);
    }

    @ShadowSources({ "vehicle", "vehicle.departureTime", "previousVisit", "previousVisit.endServiceTime" })
    public Integer endServiceTimeSupplier() {
        if (vehicle == null) {
            return null;
        }
        return (previousVisit == null ? vehicle.getDepartureTime() : previousVisit.getEndServiceTime()) + 1;
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

    public Integer getEndServiceTime() {
        return endServiceTime;
    }

    public void setEndServiceTime(Integer endServiceTime) {
        this.endServiceTime = endServiceTime;
    }
}
