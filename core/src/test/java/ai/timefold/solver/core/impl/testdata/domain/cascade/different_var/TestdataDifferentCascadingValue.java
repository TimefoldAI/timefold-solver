package ai.timefold.solver.core.impl.testdata.domain.cascade.different_var;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataDifferentCascadingValue {

    public static EntityDescriptor<TestdataDifferentCascadingSolution> buildEntityDescriptor() {
        return TestdataDifferentCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataDifferentCascadingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataDifferentCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataDifferentCascadingValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataDifferentCascadingValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadingUpdateShadowVariable(targetMethodName = "updateSecondCascadeValue")
    private Integer secondCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;
    private int secondNumberOfCalls = 0;

    public TestdataDifferentCascadingValue(Integer value) {
        this.value = value;
    }

    public TestdataDifferentCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataDifferentCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataDifferentCascadingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataDifferentCascadingValue previous) {
        this.previous = previous;
    }

    public TestdataDifferentCascadingValue getNext() {
        return next;
    }

    public void setNext(TestdataDifferentCascadingValue next) {
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

    public Integer getSecondCascadeValue() {
        return secondCascadeValue;
    }

    public void setSecondCascadeValue(Integer secondCascadeValue) {
        this.secondCascadeValue = secondCascadeValue;
    }

    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    public int getSecondNumberOfCalls() {
        return secondNumberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        numberOfCalls++;
        if (cascadeValue == null || cascadeValue != value + 1) {
            cascadeValue = value + 1;
        }
    }

    public void updateSecondCascadeValue() {
        secondNumberOfCalls++;
        if (secondCascadeValue == null || secondCascadeValue != value + 1) {
            secondCascadeValue = value + 1;
        }
    }

    @Override
    public String toString() {
        return "TestdataDifferentCascadingValue{" +
                "value=" + value +
                '}';
    }
}
