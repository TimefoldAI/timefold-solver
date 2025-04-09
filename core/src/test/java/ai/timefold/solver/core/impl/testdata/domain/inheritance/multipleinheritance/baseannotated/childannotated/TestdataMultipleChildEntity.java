package ai.timefold.solver.core.impl.testdata.domain.inheritance.multipleinheritance.baseannotated.childannotated;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.inheritance.singlevar.baseannotated.childannotated.TestdataChildEntity;

@PlanningEntity
public class TestdataMultipleChildEntity extends TestdataChildEntity {

    @PlanningVariable(valueRangeProviderRefs = "valueRange2")
    private String value2;

    public TestdataMultipleChildEntity() {
    }

    public TestdataMultipleChildEntity(long id) {
        super(id);
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }
}
