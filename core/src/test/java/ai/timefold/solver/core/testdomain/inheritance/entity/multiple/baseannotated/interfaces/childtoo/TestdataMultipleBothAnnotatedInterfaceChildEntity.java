package ai.timefold.solver.core.testdomain.inheritance.entity.multiple.baseannotated.interfaces.childtoo;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataMultipleBothAnnotatedInterfaceChildEntity implements TestdataMultipleBothAnnotatedInterfaceSecondEntity {

    private Long id;
    private String value;
    private String value2;

    public TestdataMultipleBothAnnotatedInterfaceChildEntity() {
    }

    public TestdataMultipleBothAnnotatedInterfaceChildEntity(long id) {
        this.id = id;
    }

    @Override
    public String getValue2() {
        return value2;
    }

    @Override
    public void setValue2(String value2) {
        this.value2 = value2;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }
}
