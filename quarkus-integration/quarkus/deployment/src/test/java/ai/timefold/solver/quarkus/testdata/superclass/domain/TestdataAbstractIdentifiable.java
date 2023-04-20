package ai.timefold.solver.quarkus.testdata.superclass.domain;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

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
}
