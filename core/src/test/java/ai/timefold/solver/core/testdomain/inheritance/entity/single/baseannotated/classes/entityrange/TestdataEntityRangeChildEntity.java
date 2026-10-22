package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.entityrange;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataEntityRangeChildEntity extends TestdataEntityRangeBaseEntity {

    public TestdataEntityRangeChildEntity() {
    }

    public TestdataEntityRangeChildEntity(long id, List<String> valueList) {
        super(id, valueList);
    }
}
