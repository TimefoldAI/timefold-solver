package ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevarinterface.baseannotated.childannotatedreplacevar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataChildEntity implements TestdataBaseEntity {

    private Long id;
    private String value;

    public TestdataChildEntity() {
    }

    public TestdataChildEntity(long id) {
        this.id = id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }
}
