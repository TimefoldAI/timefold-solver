package ai.timefold.solver.core.impl.testdata.domain.inheritance.solution.baseannotated.childnot;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataOnlyBaseAnnotatedChildEntity extends TestdataOnlyBaseAnnotatedBaseEntity {

    public TestdataOnlyBaseAnnotatedChildEntity() {
    }

    public TestdataOnlyBaseAnnotatedChildEntity(long id) {
        super(id);
    }
}
