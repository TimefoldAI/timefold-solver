package ai.timefold.solver.core.testdomain.shadow.method_variables;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.shadow.extended.TestdataDeclarativeExtendedBaseValue;

@PlanningEntity
public class TestdataDeclarativeMethodVariablesEntity extends TestdataObject {
    List<TestdataDeclarativeExtendedBaseValue> values;

    public TestdataDeclarativeMethodVariablesEntity() {
        super();
        this.values = new ArrayList<>();
    }

    public TestdataDeclarativeMethodVariablesEntity(String code) {
        super(code);
        this.values = new ArrayList<>();
    }

    @PlanningListVariable
    public List<TestdataDeclarativeExtendedBaseValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataDeclarativeExtendedBaseValue> values) {
        this.values = values;
    }
}
