package ai.timefold.solver.core.testdomain.shadow.mixed;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedValue extends TestdataObject {
    @PlanningVariable
    Integer delay;

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataMixedEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMixedValue previous;

    @ShadowVariable(supplierName = "previousDelaySupplier")
    private Integer previousDelay;

    public TestdataMixedValue() {
        // required for cloning
    }

    public TestdataMixedValue(String code) {
        super(code);
    }

    @ShadowSources({
            "previous",
            "previous.delay"
    })
    public Integer previousDelaySupplier() {
        if (previous == null) {
            return null;
        } else {
            return previous.delay;
        }
    }

    public Integer getDelay() {
        return delay;
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public TestdataMixedEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataMixedEntity entity) {
        this.entity = entity;
    }

    public TestdataMixedValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataMixedValue previous) {
        this.previous = previous;
    }

    public Integer getPreviousDelay() {
        return previousDelay;
    }

    public void setPreviousDelay(Integer previousDelay) {
        this.previousDelay = previousDelay;
    }
}
