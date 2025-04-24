package ai.timefold.solver.core.impl.testdata.domain.inheritance.entity.single.baseannotated.classes.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataBaseEntity {

    @PlanningId
    private Long id;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataBaseEntity() {
    }

    public TestdataBaseEntity(long id) {
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
