package ai.timefold.solver.core.testdomain.valuerange.entityproviding.multientity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * Entity whose basic variable uses only a solution value range,
 * while the other entity class of the same model uses an entity value range.
 */
@PlanningEntity
public class TestdataMultiEntitySolutionRangeEntity extends TestdataObject {

    private TestdataValue solutionValue;

    public TestdataMultiEntitySolutionRangeEntity() {
        // Required for cloning
    }

    public TestdataMultiEntitySolutionRangeEntity(String code, TestdataValue solutionValue) {
        super(code);
        this.solutionValue = solutionValue;
    }

    @PlanningVariable(valueRangeProviderRefs = "solutionValueRange")
    public TestdataValue getSolutionValue() {
        return solutionValue;
    }

    public void setSolutionValue(TestdataValue solutionValue) {
        this.solutionValue = solutionValue;
    }
}
