package ai.timefold.solver.core.testdomain.declarative.extended;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataDeclarativeExtendedSubclassValue extends TestdataDeclarativeExtendedBaseValue {
    String codeSum;

    public TestdataDeclarativeExtendedSubclassValue() {
        super();
    }

    public TestdataDeclarativeExtendedSubclassValue(String code) {
        super(code);
        codeSum = code;
    }

    @Override
    public TestdataDeclarativeExtendedSubclassValue getPrevious() {
        return (TestdataDeclarativeExtendedSubclassValue) previous;
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
