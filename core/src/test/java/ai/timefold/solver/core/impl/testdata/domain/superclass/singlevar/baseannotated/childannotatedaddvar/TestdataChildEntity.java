package ai.timefold.solver.core.impl.testdata.domain.superclass.singlevar.baseannotated.childannotatedaddvar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataChildEntity extends TestdataBaseEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange2")
    private String value2;

    public TestdataChildEntity() {
    }

    public TestdataChildEntity(long id) {
        super(id);
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
