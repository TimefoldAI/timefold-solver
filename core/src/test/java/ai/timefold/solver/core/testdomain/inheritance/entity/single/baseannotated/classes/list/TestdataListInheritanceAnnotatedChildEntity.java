package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.list;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataListInheritanceAnnotatedChildEntity extends TestdataListInheritanceBaseEntity {

    public TestdataListInheritanceAnnotatedChildEntity() {
    }

    public TestdataListInheritanceAnnotatedChildEntity(String code, TestdataValue... values) {
        super(code, values);
    }

}
