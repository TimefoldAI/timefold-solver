package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_element_sourced;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle that starts where its predecessor ends, where the predecessor is a planning variable
 * instead of a fact, so the graph around the chain nodes is not fixed: the solver could chain two vehicles
 * to each other, closing a dependency loop through their routes.
 */
@PlanningEntity
public class TestdataMultiEntityChainElementSourcedVehicle extends TestdataObject {

    @PlanningVariable(allowsUnassigned = true)
    TestdataMultiEntityChainElementSourcedVehicle previousVehicle;

    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainElementSourcedVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataMultiEntityChainElementSourcedVehicle() {
    }

    public TestdataMultiEntityChainElementSourcedVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources({ "previousVehicle", "previousVehicle.endTime" })
    public Integer startTimeSupplier() {
        if (previousVehicle == null) {
            return departureTime;
        }
        return previousVehicle.getEndTime();
    }

    @ShadowSources("visits[].endServiceTime")
    public Integer endTimeSupplier() {
        if (visits.isEmpty()) {
            // Not the startTime: reading it would declare the very source this domain is here to omit.
            return departureTime;
        }
        return visits.getLast().getEndServiceTime();
    }

    public TestdataMultiEntityChainElementSourcedVehicle getPreviousVehicle() {
        return previousVehicle;
    }

    public void setPreviousVehicle(TestdataMultiEntityChainElementSourcedVehicle previousVehicle) {
        this.previousVehicle = previousVehicle;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public List<TestdataMultiEntityChainElementSourcedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainElementSourcedVisit> visits) {
        this.visits = visits;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

}
