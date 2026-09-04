package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_loop;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle that starts where its predecessor ends, where the predecessor is a planning variable
 * instead of a fact. The solver can therefore chain two vehicles to each other, which is a
 * dependency loop it can break again; a loop between facts would fail fast at build time instead.
 */
@PlanningEntity
public class TestdataChainLoopVehicle extends TestdataObject {

    @PlanningVariable(allowsUnassigned = true)
    TestdataChainLoopVehicle previousVehicle;

    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataChainLoopVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    @ShadowVariablesInconsistent
    Boolean inconsistent;

    public TestdataChainLoopVehicle() {
    }

    public TestdataChainLoopVehicle(String code, int departureTime) {
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

    @ShadowSources({ "visits[].endServiceTime", "startTime" })
    public Integer endTimeSupplier() {
        if (visits.isEmpty()) {
            return startTime;
        }
        return visits.getLast().getEndServiceTime();
    }

    public TestdataChainLoopVehicle getPreviousVehicle() {
        return previousVehicle;
    }

    public void setPreviousVehicle(TestdataChainLoopVehicle previousVehicle) {
        this.previousVehicle = previousVehicle;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public List<TestdataChainLoopVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataChainLoopVisit> visits) {
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

    public Boolean getInconsistent() {
        return inconsistent;
    }

    public void setInconsistent(Boolean inconsistent) {
        this.inconsistent = inconsistent;
    }
}
