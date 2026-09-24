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
 * instead of a fact. The solver can therefore chain two vehicles to each other, which is a
 * dependency loop it can break again; a loop between facts would fail fast at build time instead.
 * <p>
 * Unlike {@code TestdataChainLoopVehicle}, its endTime is sourced from its route alone and never
 * reads its own startTime, so nothing declares the dependency of the endTime on the startTime.
 * The element cascade has to supply that edge itself, or the endTime is computed before the route
 * it summarizes has been walked, and the loop through the route goes undetected.
 */
@PlanningEntity
public class TestdataElementSourcedVehicle extends TestdataObject {

    @PlanningVariable(allowsUnassigned = true)
    TestdataElementSourcedVehicle previousVehicle;

    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataElementSourcedVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataElementSourcedVehicle() {
    }

    public TestdataElementSourcedVehicle(String code, int departureTime) {
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

    public TestdataElementSourcedVehicle getPreviousVehicle() {
        return previousVehicle;
    }

    public void setPreviousVehicle(TestdataElementSourcedVehicle previousVehicle) {
        this.previousVehicle = previousVehicle;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public List<TestdataElementSourcedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataElementSourcedVisit> visits) {
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
