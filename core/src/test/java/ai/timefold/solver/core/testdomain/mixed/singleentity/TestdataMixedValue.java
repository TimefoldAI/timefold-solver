package ai.timefold.solver.core.testdomain.mixed.singleentity;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
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

    @ShadowVariable(variableListenerClass = TestdataMixedVariableListener.class, sourceVariableName = "index")
    private Integer shadowVariableListenerValue;

    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadingShadowValue")
    private Integer cascadingShadowVariableValue;

    @ShadowVariable(supplierName = "updateDeclarativeShadowValue")
    private Integer declarativeShadowVariableValue;

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

    public Integer getShadowVariableListenerValue() {
        return shadowVariableListenerValue;
    }

    public void setShadowVariableListenerValue(Integer shadowVariableListenerValue) {
        this.shadowVariableListenerValue = shadowVariableListenerValue;
    }

    public Integer getCascadingShadowVariableValue() {
        return cascadingShadowVariableValue;
    }

    public void setCascadingShadowVariableValue(Integer cascadingShadowVariableValue) {
        this.cascadingShadowVariableValue = cascadingShadowVariableValue;
    }

    public Integer getDeclarativeShadowVariableValue() {
        return declarativeShadowVariableValue;
    }

    public void setDeclarativeShadowVariableValue(Integer declarativeShadowVariableValue) {
        this.declarativeShadowVariableValue = declarativeShadowVariableValue;
    }

    public void updateCascadingShadowValue() {
        if (index != null) {
            this.cascadingShadowVariableValue = index + 1;
        } else {
            this.cascadingShadowVariableValue = null;
        }
    }

    @ShadowSources("index")
    public Integer updateDeclarativeShadowValue() {
        if (index != null) {
            return index + 2;
        }
        return null;
    }
}
