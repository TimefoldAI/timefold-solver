package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_departure;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMultiEntityChainDepartureVehicle extends TestdataObject {

    @PlanningVariable
    Integer departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainDepartureVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataMultiEntityChainDepartureVehicle() {
    }

    public TestdataMultiEntityChainDepartureVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources("visits[].endServiceTime")
    public Integer endTimeSupplier() {
        return visits.isEmpty() ? departureTime : visits.getLast().getEndServiceTime();
    }

    public Integer getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Integer departureTime) {
        this.departureTime = departureTime;
    }

    public List<TestdataMultiEntityChainDepartureVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainDepartureVisit> visits) {
        this.visits = visits;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }
}
