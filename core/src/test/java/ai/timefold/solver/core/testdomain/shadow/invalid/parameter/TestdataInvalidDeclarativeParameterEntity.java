package ai.timefold.solver.core.testdomain.shadow.invalid.parameter;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataInvalidDeclarativeParameterEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataInvalidDeclarativeParameterValue> values;

    public TestdataInvalidDeclarativeParameterEntity() {
    }

    public TestdataInvalidDeclarativeParameterEntity(String code) {
        super(code);
    }

    public List<TestdataInvalidDeclarativeParameterValue> getValues() {
        return values;
    }

    public void setValues(
            List<TestdataInvalidDeclarativeParameterValue> values) {
        this.values = values;
    }
}
