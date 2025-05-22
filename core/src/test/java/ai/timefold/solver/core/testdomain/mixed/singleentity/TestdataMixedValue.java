package ai.timefold.solver.core.testdomain.mixed.singleentity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataMixedValue extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataMixedEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMixedValue previousElement;

    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMixedValue nextElement;

    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;

    public TestdataMixedValue() {
        // Required for cloner
    }

    public TestdataMixedValue(String code) {
        super(code);
    }

    public TestdataMixedEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataMixedEntity entity) {
        this.entity = entity;
    }

    public TestdataMixedValue getPreviousElement() {
        return previousElement;
    }

    public void setPreviousElement(TestdataMixedValue previousElement) {
        this.previousElement = previousElement;
    }

    public TestdataMixedValue getNextElement() {
        return nextElement;
    }

    public void setNextElement(TestdataMixedValue nextElement) {
        this.nextElement = nextElement;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
