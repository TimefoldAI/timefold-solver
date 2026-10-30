package ai.timefold.solver.service.quarkus.deployment.testdata.uniqueresource;

import ai.timefold.solver.service.definition.api.usage.UniqueResourceId;

public class TestdataVisit {

    @UniqueResourceId(type = "visit")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
