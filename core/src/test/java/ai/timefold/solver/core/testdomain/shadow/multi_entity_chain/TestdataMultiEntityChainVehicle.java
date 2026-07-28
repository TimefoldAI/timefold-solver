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

    List<TestdataMultiEntityChainVehicle> previousVehicles = new ArrayList<>();
    int departureTime;

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
        // A head vehicle's only source is an empty fact collection,
        // so its supplier is never triggered; initialize to the value it would compute.
        this.previousEndTime = departureTime;
    }

    @ShadowSources("previousVehicles[].endTime")
    public Integer previousEndTimeSupplier() {
        previousEndTimeCalledCount++;
        var max = departureTime;
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
        return visits.get(visits.size() - 1).getEndServiceTime();
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
