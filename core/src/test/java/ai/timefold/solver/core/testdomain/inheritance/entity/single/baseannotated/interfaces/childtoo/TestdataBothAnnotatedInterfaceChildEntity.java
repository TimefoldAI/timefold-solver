package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataBothAnnotatedInterfaceChildEntity implements TestdataBaseEntity {

    private Long id;
    private String value;

    public TestdataBothAnnotatedInterfaceChildEntity() {
    }

    public TestdataBothAnnotatedInterfaceChildEntity(long id) {
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
