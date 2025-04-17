package ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.multiple.baseannotated.classes.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataMultipleBothAnnotatedSecondChildEntity extends TestdataMultipleBothAnnotatedBaseEntity {

    public TestdataMultipleBothAnnotatedSecondChildEntity() {
    }

    public TestdataMultipleBothAnnotatedSecondChildEntity(long id) {
        super(id);
    }
}
