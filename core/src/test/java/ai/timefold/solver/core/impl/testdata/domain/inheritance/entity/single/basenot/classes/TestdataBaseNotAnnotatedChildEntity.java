package ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.basenot.classes;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataBaseNotAnnotatedChildEntity extends TestdataBaseNotAnnotatedBaseEntity {

    public TestdataBaseNotAnnotatedChildEntity() {
    }

    public TestdataBaseNotAnnotatedChildEntity(long id) {
        super(id);
    }
}
