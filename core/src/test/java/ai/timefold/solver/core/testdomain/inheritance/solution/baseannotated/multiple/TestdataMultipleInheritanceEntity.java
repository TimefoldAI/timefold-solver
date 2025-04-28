package ai.timefold.solver.core.testdomain.inheritance.solution.baseannotated.multiple;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataMultipleInheritanceEntity {

    @PlanningId
    private Long id;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataMultipleInheritanceEntity(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
