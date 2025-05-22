package ai.timefold.solver.core.testdomain.mixed.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiEntityFirstValue extends TestdataObject {

    public TestdataListMultiEntityFirstValue() {
        // Required for cloner
    }

    public TestdataListMultiEntityFirstValue(String code) {
        super(code);
    }
}
