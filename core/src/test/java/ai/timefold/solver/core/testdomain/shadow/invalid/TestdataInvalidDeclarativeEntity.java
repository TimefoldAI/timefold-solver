package ai.timefold.solver.core.testdomain.shadow.invalid;

import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataInvalidDeclarativeEntity extends TestdataObject {
    @PlanningListVariable
    List<TestdataInvalidDeclarativeValue> values;

    @ShadowVariable(supplierName = "shadowSupplier")
    Integer shadow;

    public TestdataInvalidDeclarativeEntity() {
    }

    public TestdataInvalidDeclarativeEntity(String code) {
        super(code);
    }

    @ShadowSources("values")
    public Integer shadowSupplier() {
        return values.size();
    }
}
