package ai.timefold.solver.core.testdomain.multivar.list.singleentity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListMultiVarValue extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataListMultiVarEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataListMultiVarValue previousElement;

    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataListMultiVarValue nextElement;

    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;

    public TestdataListMultiVarValue() {
        // Required for cloner
    }

    public TestdataListMultiVarValue(String code) {
        super(code);
    }

    public TestdataListMultiVarEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListMultiVarEntity entity) {
        this.entity = entity;
    }

    public TestdataListMultiVarValue getPreviousElement() {
        return previousElement;
    }

    public void setPreviousElement(TestdataListMultiVarValue previousElement) {
        this.previousElement = previousElement;
    }

    public TestdataListMultiVarValue getNextElement() {
        return nextElement;
    }

    public void setNextElement(TestdataListMultiVarValue nextElement) {
        this.nextElement = nextElement;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
