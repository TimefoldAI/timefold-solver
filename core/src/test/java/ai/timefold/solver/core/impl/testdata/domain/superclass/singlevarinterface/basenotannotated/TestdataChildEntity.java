package ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.basenotannotated;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

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

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
