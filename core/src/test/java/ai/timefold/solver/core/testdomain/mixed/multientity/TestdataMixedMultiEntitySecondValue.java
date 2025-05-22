package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedMultiEntitySecondValue extends TestdataObject {

    public TestdataMixedMultiEntitySecondValue() {
        // Required for cloner
    }

    public TestdataMixedMultiEntitySecondValue(String code) {
        super(code);
    }
}
