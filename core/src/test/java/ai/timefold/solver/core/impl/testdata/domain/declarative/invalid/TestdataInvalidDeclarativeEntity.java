package ai.timefold.solver.core.impl.testdata.domain.declarative.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

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
