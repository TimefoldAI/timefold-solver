package ai.timefold.solver.core.testdomain.declarative.extended;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeExtendedEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataDeclarativeExtendedBaseValue> values;

    public TestdataDeclarativeExtendedEntity() {
        super();
        this.values = new ArrayList<>();
    }

    public TestdataDeclarativeExtendedEntity(String code) {
        super(code);
        this.values = new ArrayList<>();
    }

    public List<TestdataDeclarativeExtendedBaseValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeExtendedBaseValue> values) {
        this.values = values;
    }
}
