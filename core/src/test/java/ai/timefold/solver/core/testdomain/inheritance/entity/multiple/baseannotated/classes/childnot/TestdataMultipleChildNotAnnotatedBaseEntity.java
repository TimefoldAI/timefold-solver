package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.classes.childnot;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataMultipleChildNotAnnotatedBaseEntity {

    @PlanningId
    private Long id;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataMultipleChildNotAnnotatedBaseEntity() {
    }

    public TestdataMultipleChildNotAnnotatedBaseEntity(long id) {
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
