package ai.timefold.solver.core.testdomain.inheritance.entity.single.baseannotated.classes.replacevar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataReplaceVarChildEntity extends TestdataReplaceVarBaseEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataReplaceVarChildEntity() {
    }

    public TestdataReplaceVarChildEntity(long id) {
        super(id);
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
