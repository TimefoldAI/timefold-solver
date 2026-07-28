package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_fallback;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A vehicle sourcing a declarative variable through a fact of its list variable's elements,
 * which may belong to another vehicle's elements,
 * so the multi-entity chained graph cannot represent it.
 */
@PlanningEntity
public class TestdataElementFactVehicle extends TestdataObject {

    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataElementFactVisit> visits = new ArrayList<>();

    @ShadowVariable(supplierName = "buddyEndTimeSupplier")
    Integer buddyEndTime;

    public TestdataElementFactVehicle() {
    }

    public TestdataElementFactVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    @ShadowSources("visits[].buddy.endServiceTime")
    public Integer buddyEndTimeSupplier() {
        var max = departureTime;
        for (var visit : visits) {
            var buddyEndServiceTime = visit.getBuddy().getEndServiceTime();
            if (buddyEndServiceTime == null) {
                return null;
            }
            max = Math.max(max, buddyEndServiceTime);
        }
        return max;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public List<TestdataElementFactVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataElementFactVisit> visits) {
        this.visits = visits;
    }

    public Integer getBuddyEndTime() {
        return buddyEndTime;
    }

    public void setBuddyEndTime(Integer buddyEndTime) {
        this.buddyEndTime = buddyEndTime;
    }
}
