package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle that starts where its single predecessor vehicle ends,
 * whose endTime does not read its startTime:
 * a cyclic predecessor relation is then only a dependency loop through the actual
 * elements, which the solver can break, and must not fail fast.
 */
@PlanningEntity
public class TestdataFactCycleVehicle extends TestdataObject {

    TestdataFactCycleVehicle previousVehicle;
    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataFactCycleVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    @ShadowVariablesInconsistent
    boolean inconsistent;

    public TestdataFactCycleVehicle() {
    }

    public TestdataFactCycleVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
        // A head vehicle's only source is a null fact,
        // so its supplier is never triggered; initialize to the value it would compute.
        this.startTime = departureTime;
    }

    @ShadowSources("previousVehicle.endTime")
    public Integer startTimeSupplier() {
        if (previousVehicle == null) {
            return departureTime;
        }
        if (previousVehicle.getEndTime() == null) {
            return null;
        }
        return Math.max(departureTime, previousVehicle.getEndTime());
    }

    @ShadowSources("visits[].endServiceTime")
    public Integer endTimeSupplier() {
        if (visits.isEmpty()) {
            // Deliberately reads the departureTime fact, not startTime:
            // an endTime reading startTime would make the cyclic predecessor relation
            // a genuine fixed loop instead of an element-induced one.
            return departureTime;
        }
        return visits.get(visits.size() - 1).getEndServiceTime();
    }

    public TestdataFactCycleVehicle getPreviousVehicle() {
        return previousVehicle;
    }

    public void setPreviousVehicle(TestdataFactCycleVehicle previousVehicle) {
        this.previousVehicle = previousVehicle;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public List<TestdataFactCycleVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFactCycleVisit> visits) {
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

    public boolean isInconsistent() {
        return inconsistent;
    }

    public void setInconsistent(boolean inconsistent) {
        this.inconsistent = inconsistent;
    }
}
