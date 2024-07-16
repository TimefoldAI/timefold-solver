package ai.timefold.solver.core.impl.testdata.domain.cascade;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.cascade.CascadeUpdateElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.NextElementShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.nextprev.PreviousElementShadowVariableDescriptor;

@PlanningEntity
public class TestdataCascadeValue {

    public static EntityDescriptor<TestdataCascadeSolution> buildEntityDescriptor() {
        return TestdataCascadeSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataCascadeValue.class);
    }

    public static InverseRelationShadowVariableDescriptor<TestdataCascadeSolution> buildVariableDescriptorForEntity() {
        return (InverseRelationShadowVariableDescriptor<TestdataCascadeSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("entity");
    }

    public static PreviousElementShadowVariableDescriptor<TestdataCascadeSolution> buildVariableDescriptorForPreviousElement() {
        return (PreviousElementShadowVariableDescriptor<TestdataCascadeSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("previous");
    }

    public static NextElementShadowVariableDescriptor<TestdataCascadeSolution> buildVariableDescriptorForNextElement() {
        return (NextElementShadowVariableDescriptor<TestdataCascadeSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("next");
    }

    public static CascadeUpdateElementShadowVariableDescriptor<TestdataCascadeSolution>
            buildVariableDescriptorForCascadeValue() {
        return (CascadeUpdateElementShadowVariableDescriptor<TestdataCascadeSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("cascadeValue");
    }

    public static CascadeUpdateElementShadowVariableDescriptor<TestdataCascadeSolution>
            buildVariableDescriptorForCascadeValueReturnType() {
        return (CascadeUpdateElementShadowVariableDescriptor<TestdataCascadeSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("cascadeValueReturnType");
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeValue next;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValueWithReturnType")
    private Integer cascadeValueReturnType;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataCascadeValue(Integer value) {
        this.value = value;
    }

    public TestdataCascadeEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataCascadeEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadeValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadeValue previous) {
        this.previous = previous;
    }

    public TestdataCascadeValue getNext() {
        return next;
    }

    public void setNext(TestdataCascadeValue next) {
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

    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        numberOfCalls++;
        if (cascadeValue == null || cascadeValue != value + 1) {
            cascadeValue = value + 1;
        }
    }

    public Integer updateCascadeValueWithReturnType() {
        if (cascadeValueReturnType == null || cascadeValueReturnType != value + 1) {
            cascadeValueReturnType = value + 1;
        }
        return cascadeValueReturnType;
    }

    public void reset() {
        numberOfCalls = 0;
        cascadeValue = null;
        cascadeValueReturnType = null;
    }

    @Override
    public String toString() {
        return "TestdataCascadeValue{" +
                "value=" + value +
                '}';
    }
}
