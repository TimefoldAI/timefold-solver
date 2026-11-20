package ai.timefold.solver.core.testdomain.pinned.solutionargument;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

@PlanningEntity
public class TestdataPinFromSolutionEntity extends TestdataObject {

    private TestdataValue value;
    private boolean pinned;
    private int startTime;
    private int solutionSetterCallCount;

    public TestdataPinFromSolutionEntity() {
    }

    public TestdataPinFromSolutionEntity(String code, int startTime) {
        super(code);
        this.startTime = startTime;
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

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void setPinned(TestdataPinFromSolutionSolution solution) {
        solutionSetterCallCount++;
        this.pinned = startTime < solution.getPlanningWindowStart();
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getSolutionSetterCallCount() {
        return solutionSetterCallCount;
    }

}
