package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.addvar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataAddVarInterfaceChildEntity implements TestdataAddVarInterfaceBaseEntity {

    private Long id;
    private String value;
    @PlanningVariable(valueRangeProviderRefs = "valueRange2")
    private String value2;

    public TestdataAddVarInterfaceChildEntity() {
    }

    public TestdataAddVarInterfaceChildEntity(long id) {
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

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
