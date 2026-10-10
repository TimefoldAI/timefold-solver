package ai.timefold.solver.core.testdomain.pinned.solutionargument;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataPinFromSolutionInvalidEntity extends TestdataObject {

    private TestdataValue value;
    private boolean pinned;

    public TestdataPinFromSolutionInvalidEntity() {
    }

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataValue getValue() {
        return value;
    }

    public void setValue(TestdataValue value) {
        this.value = value;
    }

    @PlanningPin
    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(String ignored) {
        this.pinned = true;
    }

}
