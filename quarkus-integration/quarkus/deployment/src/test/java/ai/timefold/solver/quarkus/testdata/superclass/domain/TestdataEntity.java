package ai.timefold.solver.quarkus.testdata.superclass.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataEntity extends TestdataAbstractIdentifiable {

    private String value;

    public TestdataEntity() {
    }

    public TestdataEntity(long id) {
        super(id);
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
