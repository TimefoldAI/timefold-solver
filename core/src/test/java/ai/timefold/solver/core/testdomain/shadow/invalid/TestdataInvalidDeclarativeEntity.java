package ai.timefold.solver.core.testdomain.shadow.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataInvalidDeclarativeEntity extends TestdataObject {
    TestdataInvalidDeclarativeEntity fact;

    @PlanningListVariable
    List<TestdataInvalidDeclarativeValue> values;

    @ShadowVariable(supplierName = "shadowSupplier")
    Integer shadow;

    public TestdataInvalidDeclarativeEntity() {
    }

    public TestdataInvalidDeclarativeEntity(String code) {
        super(code);
    }

    @ShadowSources("fact.shadow")
    public Integer shadowSupplier() {
        return fact == null ? 0 : fact.getShadow();
    }

    public TestdataInvalidDeclarativeEntity getFact() {
        return fact;
    }

    public void setFact(TestdataInvalidDeclarativeEntity fact) {
        this.fact = fact;
    }

    public List<TestdataInvalidDeclarativeValue> getValues() {
        return values;
    }

    public void setValues(List<TestdataInvalidDeclarativeValue> values) {
        this.values = values;
    }

    public Integer getShadow() {
        return shadow;
    }

    public void setShadow(Integer shadow) {
        this.shadow = shadow;
    }
}
