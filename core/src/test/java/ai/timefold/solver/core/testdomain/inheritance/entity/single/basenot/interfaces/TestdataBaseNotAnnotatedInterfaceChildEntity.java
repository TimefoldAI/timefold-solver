package ai.timefold.solver.core.testdomain.inheritance.entity.single.basenot.interfaces;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

@PlanningEntity
public class TestdataBaseNotAnnotatedInterfaceChildEntity implements TestdataBaseNotAnnotatedInterfaceBaseEntity {

    private Long id;
    private String value;

    public TestdataBaseNotAnnotatedInterfaceChildEntity(int id) {
        this.id = (long) id;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
