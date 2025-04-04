package ai.timefold.solver.core.impl.testdata.domain.superclass.multipleinheritance.baseannotated.mixed.childannotated;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.testdata.domain.superclass.singlevarinterface.baseannotated.childannotated.TestdataBaseEntity;

@PlanningEntity
public class TestdataMultipleChildEntity extends TestdataMultipleBaseEntity implements TestdataBaseEntity {

    private Long id;
    private String value;
    @PlanningVariable(valueRangeProviderRefs = "valueRange3")
    private String value3;

    public TestdataMultipleChildEntity() {
    }

    public TestdataMultipleChildEntity(long id) {
        this.id = id;
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

    public String getValue3() {
        return value3;
    }

    public void setValue3(String value3) {
        this.value3 = value3;
    }
}
