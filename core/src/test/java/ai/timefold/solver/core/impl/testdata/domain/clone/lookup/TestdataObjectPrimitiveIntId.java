package ai.timefold.solver.core.impl.testdata.domain.clone.lookup;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

public class TestdataObjectPrimitiveIntId {

    @PlanningId
    private final int id;

    public TestdataObjectPrimitiveIntId(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

}
