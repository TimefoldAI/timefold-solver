package ai.timefold.solver.core.testdomain.shadow.fact_group_source;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * The start time of a vehicle without previous vehicles has no source that can ever change.
 */
@PlanningEntity
public class TestdataFactGroupSourceVehicle extends TestdataObject {

    List<TestdataFactGroupSourceVehicle> previousVehicles = new ArrayList<>();

    @PlanningListVariable
    List<TestdataFactGroupSourceVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    public TestdataFactGroupSourceVehicle() {
    }

    public TestdataFactGroupSourceVehicle(String code) {
        super(code);
    }

    @ShadowSources("previousVehicles[].startTime")
    public Integer startTimeSupplier() {
        var start = 0;
        for (var previousVehicle : previousVehicles) {
            start = Math.max(start, previousVehicle.getStartTime() + 1);
        }
        return start;
    }

    public List<TestdataFactGroupSourceVehicle> getPreviousVehicles() {
        return previousVehicles;
    }

    public List<TestdataFactGroupSourceVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFactGroupSourceVisit> visits) {
        this.visits = visits;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }
}
