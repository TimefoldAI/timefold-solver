package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle that starts where its predecessor vehicles end,
 * so a change on a predecessor's route propagates to this vehicle's visits.
 */
@PlanningEntity
public class TestdataMultiEntityChainVehicle extends TestdataObject {

    // Null for a head vehicle, exercising null fact collection support.
    List<TestdataMultiEntityChainVehicle> previousVehicles;
    // A planning variable, the earliest previousEndTime of the vehicle.
    @PlanningVariable
    Integer departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "previousEndTimeSupplier")
    Integer previousEndTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    // Shares a node with endTime once the graph merges the nodes of each entity,
    // which renumbers the nodes created after it, such as the chain nodes.
    @ShadowVariable(supplierName = "routeDurationSupplier")
    Integer routeDuration;

    int previousEndTimeCalledCount = 0;
    int endTimeCalledCount = 0;

    public TestdataMultiEntityChainVehicle() {
    }

    public TestdataMultiEntityChainVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources({ "previousVehicles[].endTime", "departureTime" })
    public Integer previousEndTimeSupplier() {
        previousEndTimeCalledCount++;
        var max = departureTime;
        if (previousVehicles == null) {
            return max;
        }
        for (var previousVehicle : previousVehicles) {
            if (previousVehicle.getEndTime() == null) {
                return null;
            }
            max = Math.max(max, previousVehicle.getEndTime());
        }
        return max;
    }

    @ShadowSources({ "visits[].endServiceTime", "previousEndTime" })
    public Integer endTimeSupplier() {
        endTimeCalledCount++;
        if (visits.isEmpty()) {
            return previousEndTime;
        }
        return visits.getLast().getEndServiceTime();
    }

    @ShadowSources({ "endTime", "previousEndTime" })
    public Integer routeDurationSupplier() {
        if (endTime == null || previousEndTime == null) {
            return null;
        }
        return endTime - previousEndTime;
    }

    public List<TestdataMultiEntityChainVehicle> getPreviousVehicles() {
        return previousVehicles;
    }

    public void setPreviousVehicles(List<TestdataMultiEntityChainVehicle> previousVehicles) {
        this.previousVehicles = previousVehicles;
    }

    public Integer getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(Integer departureTime) {
        this.departureTime = departureTime;
    }

    public List<TestdataMultiEntityChainVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainVisit> visits) {
        this.visits = visits;
    }

    public Integer getPreviousEndTime() {
        return previousEndTime;
    }

    public void setPreviousEndTime(Integer previousEndTime) {
        this.previousEndTime = previousEndTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

    public Integer getRouteDuration() {
        return routeDuration;
    }

    public void setRouteDuration(Integer routeDuration) {
        this.routeDuration = routeDuration;
    }

    public int getPreviousEndTimeCalledCount() {
        return previousEndTimeCalledCount;
    }

    public int getEndTimeCalledCount() {
        return endTimeCalledCount;
    }

    public void reset() {
        previousEndTimeCalledCount = 0;
        endTimeCalledCount = 0;
    }
}
