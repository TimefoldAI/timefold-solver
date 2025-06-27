package ai.timefold.solver.core.testdomain.declarative.counting;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataCountingValue extends TestdataObject {
    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataCountingValue previous;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataCountingEntity entity;

    @ShadowVariable(supplierName = "countSupplier")
    Integer count;

    int calledCount = 0;

    public TestdataCountingValue() {
    }

    public TestdataCountingValue(String code) {
        super(code);
    }

    public TestdataCountingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCountingValue previous) {
        this.previous = previous;
    }

    public TestdataCountingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataCountingEntity entity) {
        this.entity = entity;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @ShadowSources({ "previous.count", "entity" })
    public Integer countSupplier() {
        if (calledCount != 0) {
            throw new IllegalStateException("Supplier for entity %s was already called."
                    .formatted(entity));
        }
        calledCount++;
        if (entity == null) {
            return null;
        }
        if (previous == null) {
            return 0;
        }
        return previous.count + 1;
    }

    public void reset() {
        calledCount = 0;
    }
}
