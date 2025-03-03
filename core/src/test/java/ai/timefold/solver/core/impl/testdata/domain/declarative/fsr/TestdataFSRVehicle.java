package ai.timefold.solver.core.impl.testdata.domain.declarative.fsr;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class TestdataFSRVehicle {
    String id;

    @PlanningListVariable
    List<TestdataFSRVisit> visits;

    public TestdataFSRVehicle() {
        visits = new ArrayList<>();
    }

    public TestdataFSRVehicle(String id) {
        this.id = id;
        visits = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataFSRVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataFSRVisit> visits) {
        this.visits = visits;
    }

    public void updateVisitShadows() {
        TestdataFSRVisit previousVisit = null;
        for (var visit : visits) {
            visit.setVehicle(this);
            visit.setPreviousVisit(previousVisit);
            if (previousVisit != null) {
                previousVisit.setNextVisit(visit);
            }
            previousVisit = visit;
        }
        if (previousVisit != null) {
            previousVisit.setNextVisit(null);
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
