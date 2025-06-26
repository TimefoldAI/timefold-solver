package ai.timefold.solver.core.testdomain.declarative.counting;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataCountingEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataCountingValue> values;

    public TestdataCountingEntity() {
        values = new ArrayList<>();
    }

    public TestdataCountingEntity(String code) {
        super(code);
        values = new ArrayList<>();
    }

    public List<TestdataCountingValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataCountingValue> values) {
        this.values = values;
    }
}
