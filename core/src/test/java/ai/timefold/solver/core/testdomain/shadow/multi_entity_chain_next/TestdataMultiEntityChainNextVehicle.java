package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_next;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle whose latest start time is bound by its successor vehicles,
 * so a change on a successor's route propagates backwards to this vehicle's visits.
 */
@PlanningEntity
public class TestdataMultiEntityChainNextVehicle extends TestdataObject {

    List<TestdataMultiEntityChainNextVehicle> nextVehicles = new ArrayList<>();
    int deadline;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataMultiEntityChainNextVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "nextStartTimeSupplier")
    Integer nextStartTime;

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    public TestdataMultiEntityChainNextVehicle() {
    }

    public TestdataMultiEntityChainNextVehicle(String code, int deadline) {
        super(code);
        this.deadline = deadline;
        // A tail vehicle's only source is an empty fact collection,
        // so its supplier is never triggered; initialize to the value it would compute.
        this.nextStartTime = deadline;
    }

    @ShadowSources("nextVehicles[].startTime")
    public Integer nextStartTimeSupplier() {
        var min = deadline;
        for (var nextVehicle : nextVehicles) {
            if (nextVehicle.getStartTime() == null) {
                return null;
            }
            min = Math.min(min, nextVehicle.getStartTime());
        }
        return min;
    }

    @ShadowSources({ "visits[].latestStartTime", "nextStartTime" })
    public Integer startTimeSupplier() {
        if (visits.isEmpty()) {
            return nextStartTime;
        }
        return visits.get(0).getLatestStartTime();
    }

    public List<TestdataMultiEntityChainNextVehicle> getNextVehicles() {
        return nextVehicles;
    }

    public void setNextVehicles(List<TestdataMultiEntityChainNextVehicle> nextVehicles) {
        this.nextVehicles = nextVehicles;
    }

    public int getDeadline() {
        return deadline;
    }

    public void setDeadline(int deadline) {
        this.deadline = deadline;
    }

    public List<TestdataMultiEntityChainNextVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataMultiEntityChainNextVisit> visits) {
        this.visits = visits;
    }

    public Integer getNextStartTime() {
        return nextStartTime;
    }

    public void setNextStartTime(Integer nextStartTime) {
        this.nextStartTime = nextStartTime;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }
}
