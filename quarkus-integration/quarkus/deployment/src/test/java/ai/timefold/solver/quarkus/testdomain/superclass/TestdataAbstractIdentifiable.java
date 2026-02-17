package ai.timefold.solver.quarkus.testdomain.superclass;

import ai.timefold.solver.core.api.domain.common.PlanningId;

abstract class TestdataAbstractIdentifiable {

    private Long id;

    public TestdataAbstractIdentifiable() {
    }

    public TestdataAbstractIdentifiable(long id) {
        this.id = id;
    }

    @PlanningId
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
