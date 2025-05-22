package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedMultiEntityFirstValue extends TestdataObject {

    public TestdataMixedMultiEntityFirstValue() {
        // Required for cloner
    }

    public TestdataMixedMultiEntityFirstValue(String code) {
        super(code);
    }
}
