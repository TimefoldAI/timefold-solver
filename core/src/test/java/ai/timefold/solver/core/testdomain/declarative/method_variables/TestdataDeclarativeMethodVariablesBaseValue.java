package ai.timefold.solver.core.testdomain.declarative.method_variables;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeMethodVariablesBaseValue extends TestdataObject {
    TestdataDeclarativeMethodVariablesBaseValue previous;

    public TestdataDeclarativeMethodVariablesBaseValue() {
        super();
    }

    public TestdataDeclarativeMethodVariablesBaseValue(String code) {
        super(code);
    }

    @PreviousElementShadowVariable(sourceVariableName = "values")
    public TestdataDeclarativeMethodVariablesBaseValue getPrevious() {
        return previous;
    }

    public void setPrevious(
            TestdataDeclarativeMethodVariablesBaseValue previous) {
        this.previous = previous;
    }
}
