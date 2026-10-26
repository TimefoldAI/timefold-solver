package ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.service.definition.api.usage.ResourceId;

@PlanningEntity
public class TestdataVehicle {

    @ResourceId(type = "vehicle")
    private String id;

    @PlanningListVariable(valueRangeProviderRefs = { "valueRange" })
    private List<TestdataVisit> visits;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<TestdataVisit> getVisits() {
        return visits;
    }

    public void setVisits(List<TestdataVisit> visits) {
        this.visits = visits;
    }
}
