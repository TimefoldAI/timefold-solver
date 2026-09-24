package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
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
    int departureTime;
    // A vehicle cannot finish before it starts. Unlike the plain last visit's end time,
    // this makes endTime change as soon as previousEndTime does, before the chain has been walked.
    boolean endTimeIncludesPreviousEndTime = false;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "previousEndTimeSupplier")
    Integer previousEndTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    int previousEndTimeCalledCount = 0;
    int endTimeCalledCount = 0;

    public TestdataMultiEntityChainVehicle() {
    }

    public TestdataMultiEntityChainVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources("previousVehicles[].endTime")
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
        var lastEndServiceTime = visits.getLast().getEndServiceTime();
        if (!endTimeIncludesPreviousEndTime || lastEndServiceTime == null || previousEndTime == null) {
            return lastEndServiceTime;
        }
        return Math.max(previousEndTime, lastEndServiceTime);
    }

    public List<TestdataMultiEntityChainVehicle> getPreviousVehicles() {
        return previousVehicles;
    }

    public void setPreviousVehicles(List<TestdataMultiEntityChainVehicle> previousVehicles) {
        this.previousVehicles = previousVehicles;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public void setEndTimeIncludesPreviousEndTime(boolean endTimeIncludesPreviousEndTime) {
        this.endTimeIncludesPreviousEndTime = endTimeIncludesPreviousEndTime;
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
