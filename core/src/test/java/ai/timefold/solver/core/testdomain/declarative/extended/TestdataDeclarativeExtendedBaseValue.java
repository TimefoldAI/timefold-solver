package ai.timefold.solver.core.testdomain.declarative.extended;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeExtendedBaseValue extends TestdataObject {
    TestdataDeclarativeExtendedBaseValue previous;

    public TestdataDeclarativeExtendedBaseValue() {
        super();
    }

    public TestdataDeclarativeExtendedBaseValue(String code) {
        super(code);
    }

    @PreviousElementShadowVariable(sourceVariableName = "values")
    public TestdataDeclarativeExtendedBaseValue getPrevious() {
        return previous;
    }

    public void setPrevious(
            TestdataDeclarativeExtendedBaseValue previous) {
        this.previous = previous;
    }
}
