package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataSingleCascadeValue {

    public static EntityDescriptor<TestdataSingleCascadeSolution> buildEntityDescriptor() {
        return TestdataSingleCascadeSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadeValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadeEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadeValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadeValue next;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValueWithReturnType")
    private Integer cascadeValueReturnType;
    private Integer value;
    private int firstNumberOfCalls = 0;
    private int secondNumberOfCalls = 0;

    public TestdataSingleCascadeValue(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadeEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadeEntity entity) {
        this.entity = entity;
    }

    public TestdataSingleCascadeValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataSingleCascadeValue previous) {
        this.previous = previous;
    }

    public TestdataSingleCascadeValue getNext() {
        return next;
    }

    public void setNext(TestdataSingleCascadeValue next) {
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
