package ai.timefold.solver.core.testdomain.shadow.multi_entity_chain_non_owner;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * Owns the list variable but has no declarative shadow variables,
 * so the declarative entity classes are the visit and the depot.
 */
@PlanningEntity
public class TestdataNonOwnerVehicle extends TestdataObject {

    int departureTime;

    @PlanningListVariable(allowsUnassignedValues = true)
    List<TestdataNonOwnerVisit> visits = new ArrayList<>();

    public TestdataNonOwnerVehicle() {
    }

    public TestdataNonOwnerVehicle(String code, int departureTime) {
        super(code);
        this.departureTime = departureTime;
    }

    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public List<TestdataNonOwnerVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataNonOwnerVisit> visits) {
        this.visits = visits;
    }
}
