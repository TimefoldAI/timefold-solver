package ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataChildEntity extends TestdataBaseEntity {

    public TestdataChildEntity() {
    }

    public TestdataChildEntity(long id) {
        super(id);
    }
}
