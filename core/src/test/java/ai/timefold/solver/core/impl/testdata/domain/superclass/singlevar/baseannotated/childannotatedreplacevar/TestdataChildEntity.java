package ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedreplacevar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataChildEntity extends TestdataBaseEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    private String value;

    public TestdataChildEntity() {
    }

    public TestdataChildEntity(long id) {
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
