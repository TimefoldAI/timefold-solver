package ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataBothAnnotatedChildEntity extends TestdataBothAnnotatedBaseEntity {

    public TestdataBothAnnotatedChildEntity() {
    }

    public TestdataBothAnnotatedChildEntity(long id) {
        super(id);
    }
}
