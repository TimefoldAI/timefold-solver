package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.childtooabstract;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataBothAnnotatedAbstractChildEntity extends TestdataBothAnnotatedAbstractBaseEntity {

    public TestdataBothAnnotatedAbstractChildEntity() {
    }

    public TestdataBothAnnotatedAbstractChildEntity(long id) {
        super(id);
    }
}
