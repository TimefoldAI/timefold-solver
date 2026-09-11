package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fact;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle that starts where its single predecessor vehicle ends,
 * linked by a plain fact instead of a fact collection,
 * so a change on the predecessor's route propagates through a fact path.
 */
@PlanningEntity
public class TestdataFactChainVehicle extends TestdataObject {

    TestdataFactChainVehicle previousVehicle;
    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataFactChainVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    int startTimeCalledCount = 0;
    int endTimeCalledCount = 0;

    public TestdataFactChainVehicle() {
    }

    public TestdataFactChainVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources("previousVehicle.endTime")
    public Integer startTimeSupplier() {
        startTimeCalledCount++;
        if (previousVehicle == null) {
            return departureTime;
        }
        if (previousVehicle.getEndTime() == null) {
            return null;
        }
        return Math.max(departureTime, previousVehicle.getEndTime());
    }

    @ShadowSources({ "visits[].endServiceTime", "startTime" })
    public Integer endTimeSupplier() {
        endTimeCalledCount++;
        if (visits.isEmpty()) {
            return startTime;
        }
        return visits.getLast().getEndServiceTime();
    }

    public TestdataFactChainVehicle getPreviousVehicle() {
        return previousVehicle;
    }

    public void setPreviousVehicle(TestdataFactChainVehicle previousVehicle) {
        this.previousVehicle = previousVehicle;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public List<TestdataFactChainVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFactChainVisit> visits) {
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

    public int getStartTimeCalledCount() {
        return startTimeCalledCount;
    }

    public int getEndTimeCalledCount() {
        return endTimeCalledCount;
    }

    public void reset() {
        startTimeCalledCount = 0;
        endTimeCalledCount = 0;
    }
}
