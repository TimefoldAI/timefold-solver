package ai.timefold.solver.core.testdomain.multivar.list.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiEntitySecondValue extends TestdataObject {

    public TestdataListMultiEntitySecondValue() {
        // Required for cloner
    }

    public TestdataListMultiEntitySecondValue(String code) {
        super(code);
    }
}
