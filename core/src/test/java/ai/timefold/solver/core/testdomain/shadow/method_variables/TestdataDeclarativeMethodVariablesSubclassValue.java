package ai.timefold.solver.core.testdomain.shadow.method_variables;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariablesInconsistent;

@PlanningEntity
public class TestdataDeclarativeMethodVariablesSubclassValue extends TestdataDeclarativeMethodVariablesBaseValue {
    String codeSum;

    // TODO: Remove me when supplier present
    @ShadowVariablesInconsistent
    boolean isInconsistent;

    public TestdataDeclarativeMethodVariablesSubclassValue() {
        super();
    }

    public TestdataDeclarativeMethodVariablesSubclassValue(String code) {
        super(code);
        codeSum = code;
    }

    @Override
    public TestdataDeclarativeMethodVariablesSubclassValue getPrevious() {
        return (TestdataDeclarativeMethodVariablesSubclassValue) previous;
    }

    @ShadowVariable(supplierName = "codeSumSupplier")
    public String getCodeSum() {
        return codeSum;
    }

    public void setCodeSum(String codeSum) {
        this.codeSum = codeSum;
    }

    @ShadowSources("previous.codeSum")
    public String codeSumSupplier() {
        if (previous == null) {
            return code;
        } else {
            return getPrevious().codeSum + code;
        }
    }
}
