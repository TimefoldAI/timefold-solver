package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.interfaces.replacevar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataReplaceVarInterfaceChildEntity implements TestdataReplaceVarInterfaceBaseEntity {

    private Long id;
    private String value;

    public TestdataReplaceVarInterfaceChildEntity() {
    }

    public TestdataReplaceVarInterfaceChildEntity(long id) {
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
