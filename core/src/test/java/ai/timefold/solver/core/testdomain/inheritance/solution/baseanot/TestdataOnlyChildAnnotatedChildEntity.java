package ai.timefold.solver.core.testdomain.inheritance.solution.baseanot;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataOnlyChildAnnotatedChildEntity extends TestdataOnlyAnnotatedBaseEntity {

    public TestdataOnlyChildAnnotatedChildEntity() {
    }

    public TestdataOnlyChildAnnotatedChildEntity(long id) {
        super(id);
    }
}
