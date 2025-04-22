package ai.timefold.solver.core.impl.testdata.domain.declarative.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

@PlanningEntity
public class TestdataInvalidDeclarativeValue extends TestdataObject {
    TestdataInvalidDeclarativeValue fact;

    List<TestdataInvalidDeclarativeValue> group;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataInvalidDeclarativeValue previous;

    @ShadowVariable(supplierName = "dependencySupplier")
    TestdataInvalidDeclarativeValue dependency;

    @ShadowVariable(supplierName = "shadowSupplier")
    TestdataInvalidDeclarativeValue shadow;

    public TestdataInvalidDeclarativeValue() {
    }

    public TestdataInvalidDeclarativeValue(String code) {
        super(code);
    }

    public TestdataInvalidDeclarativeValue getFact() {
        return fact;
    }

    public void setFact(TestdataInvalidDeclarativeValue fact) {
        this.fact = fact;
    }

    public List<TestdataInvalidDeclarativeValue> getGroup() {
        return group;
    }

    public void setGroup(List<TestdataInvalidDeclarativeValue> group) {
        this.group = group;
    }

    public TestdataInvalidDeclarativeValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataInvalidDeclarativeValue previous) {
        this.previous = previous;
    }

    public TestdataInvalidDeclarativeValue getDependency() {
        return dependency;
    }

    public void setDependency(TestdataInvalidDeclarativeValue dependency) {
        this.dependency = dependency;
    }

    public TestdataInvalidDeclarativeValue getShadow() {
        return shadow;
    }

    public void setShadow(TestdataInvalidDeclarativeValue shadow) {
        this.shadow = shadow;
    }

    @ShadowSources("previous")
    public TestdataInvalidDeclarativeValue dependencySupplier() {
        return previous;
    }

    @ShadowSources("dependency")
    public TestdataInvalidDeclarativeValue shadowSupplier() {
        return dependency;
    }
}
