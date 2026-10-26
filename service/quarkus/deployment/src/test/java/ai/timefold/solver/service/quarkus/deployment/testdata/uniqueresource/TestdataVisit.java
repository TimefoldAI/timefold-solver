package ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource;

import ai.timefold.solver.service.definition.api.usage.ResourceId;

public class TestdataVisit {

    @ResourceId(type = "visit")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
