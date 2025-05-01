package ai.timefold.solver.core.testdomain.declarative.method_variables;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeMethodVariablesEntity extends TestdataObject {
    List<TestdataDeclarativeMethodVariablesBaseValue> values;

    public TestdataDeclarativeMethodVariablesEntity() {
        super();
        this.values = new ArrayList<>();
    }

    public TestdataDeclarativeMethodVariablesEntity(String code) {
        super(code);
        this.values = new ArrayList<>();
    }

    @PlanningListVariable
    public List<TestdataDeclarativeMethodVariablesBaseValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeMethodVariablesBaseValue> values) {
        this.values = values;
    }
}
