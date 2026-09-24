package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle whose visits are not all of the same declarative entity class,
 * {@link TestdataExtendedPriorityVisit} declaring a declarative shadow variable of its own.
 */
@PlanningEntity
public class TestdataExtendedVehicle extends TestdataObject {

    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataExtendedVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataExtendedVehicle() {
    }

    public TestdataExtendedVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources("visits[].endServiceTime")
    public Integer endTimeSupplier() {
        if (visits.isEmpty()) {
            return departureTime;
        }
        return visits.getLast().getEndServiceTime();
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public List<TestdataExtendedVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataExtendedVisit> visits) {
        this.visits = visits;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

}
