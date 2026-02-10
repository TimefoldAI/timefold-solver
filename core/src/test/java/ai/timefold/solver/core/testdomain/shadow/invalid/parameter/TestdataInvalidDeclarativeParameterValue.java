package ai.timefold.solver.core.testdomain.shadow.invalid.parameter;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataInvalidDeclarativeParameterValue extends TestdataObject {
    TestdataInvalidDeclarativeParameterValue fact;

    List<TestdataInvalidDeclarativeParameterValue> group;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataInvalidDeclarativeParameterValue previous;

    @ShadowVariable(supplierName = "shadowInvalidParameter")
    TestdataInvalidDeclarativeParameterValue invalidParameter;

    @ShadowVariablesInconsistent
    boolean inconsistent;

    public TestdataInvalidDeclarativeParameterValue() {
    }

    public TestdataInvalidDeclarativeParameterValue(String code) {
        super(code);
    }

    public TestdataInvalidDeclarativeParameterValue getFact() {
        return fact;
    }

    public void setFact(TestdataInvalidDeclarativeParameterValue fact) {
        this.fact = fact;
    }

    public List<TestdataInvalidDeclarativeParameterValue> getGroup() {
        return group;
    }

    public void setGroup(List<TestdataInvalidDeclarativeParameterValue> group) {
        this.group = group;
    }

    public TestdataInvalidDeclarativeParameterValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataInvalidDeclarativeParameterValue previous) {
        this.previous = previous;
    }

    public TestdataInvalidDeclarativeParameterValue getInvalidParameter() {
        return invalidParameter;
    }

    public void setInvalidParameter(TestdataInvalidDeclarativeParameterValue invalidParameter) {
        this.invalidParameter = invalidParameter;
    }

    public boolean isInconsistent() {
        return inconsistent;
    }

    public void setInconsistent(boolean inconsistent) {
        this.inconsistent = inconsistent;
    }

    @ShadowSources("previous")
    public TestdataInvalidDeclarativeParameterValue shadowInvalidParameter(Integer badParam) {
        return null;
    }
}
