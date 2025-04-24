package ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataBothAnnotatedChildEntity extends TestdataBaseEntity {

    public TestdataBothAnnotatedChildEntity() {
    }

    public TestdataBothAnnotatedChildEntity(long id) {
        super(id);
    }
}
