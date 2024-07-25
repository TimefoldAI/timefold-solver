package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade.piggy_back;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataInvalidSourceCascadingValue {

    public static EntityDescriptor<TestdataInvalidSourceCascadingSolution> buildEntityDescriptor() {
        return TestdataInvalidSourceCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataInvalidSourceCascadingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataInvalidSourceCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataInvalidSourceCascadingValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataInvalidSourceCascadingValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableName = "previous")
    private Integer cascadeValue;
    // Having a field with the same name as the targetMethodName should not cause any failure
    // when parsing @CascadingUpdateShadowVariable
    @SuppressWarnings("unused")
    private Integer updateCascadeValue;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValueWithReturnType", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValueWithReturnType", sourceVariableName = "previous")
    private Integer cascadeValueReturnType;
    @PiggybackShadowVariable(shadowVariableName = "cascadeValue",
            shadowEntityClass = TestdataInvalidSourceCascadingValue2.class)
    private Integer value;
    private int firstNumberOfCalls = 0;
    private int secondNumberOfCalls = 0;

    public TestdataInvalidSourceCascadingValue(Integer value) {
        this.value = value;
    }

    public TestdataInvalidSourceCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataInvalidSourceCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataInvalidSourceCascadingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataInvalidSourceCascadingValue previous) {
        this.previous = previous;
    }

    public TestdataInvalidSourceCascadingValue getNext() {
        return next;
    }

    public void setNext(TestdataInvalidSourceCascadingValue next) {
        this.next = next;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public void setCascadeValue(Integer cascadeValue) {
        this.cascadeValue = cascadeValue;
    }

    public Integer getCascadeValue() {
        return cascadeValue;
    }

    public Integer getCascadeValueReturnType() {
        return cascadeValueReturnType;
    }

    public int getFirstNumberOfCalls() {
        return firstNumberOfCalls;
    }

    public int getSecondNumberOfCalls() {
        return secondNumberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        firstNumberOfCalls++;
        if (cascadeValue == null || cascadeValue != value + 1) {
            cascadeValue = value + 1;
        }
    }

    public void updateCascadeValue(@SuppressWarnings("unused") int i) {
        // Overloaded methods should not cause any failures when parsing @CascadingUpdateShadowVariable
    }

    public Integer updateCascadeValueWithReturnType() {
        secondNumberOfCalls++;
        if (cascadeValueReturnType == null || cascadeValueReturnType != value + 2) {
            cascadeValueReturnType = value + 2;
        }
        return cascadeValueReturnType;
    }

    @Override
    public String toString() {
        return "TestdataCascadeValue{" +
                "value=" + value +
                '}';
    }
}
