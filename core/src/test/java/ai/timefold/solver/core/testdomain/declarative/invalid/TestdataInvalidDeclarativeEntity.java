package ai.timefold.solver.core.testdomain.declarative.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataInvalidDeclarativeEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataInvalidDeclarativeValue> values;

    public TestdataInvalidDeclarativeEntity() {
    }

    public TestdataInvalidDeclarativeEntity(String code) {
        super(code);
    }
}
