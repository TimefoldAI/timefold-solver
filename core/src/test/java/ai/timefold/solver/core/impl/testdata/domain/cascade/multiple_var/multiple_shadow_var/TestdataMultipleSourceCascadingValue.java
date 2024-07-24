package ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.multiple_shadow_var;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.multiple_var.TestdataMultipleCascadingBaseValue;

@PlanningEntity
public class TestdataMultipleSourceCascadingValue
        implements TestdataMultipleCascadingBaseValue<TestdataMultipleSourceCascadingEntity> {

    public static EntityDescriptor<TestdataMultipleSourceCascadingSolution> buildEntityDescriptor() {
        return TestdataMultipleSourceCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataMultipleSourceCascadingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleSourceCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleSourceCascadingValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataMultipleSourceCascadingValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableNames = { "entity", "previous" })
    private Integer cascadeValue;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue", sourceVariableNames = { "entity", "previous" })
    private Integer secondCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataMultipleSourceCascadingValue(Integer value) {
        this.value = value;
    }

    public TestdataMultipleSourceCascadingEntity getEntity() {
        return entity;
    }

    @Override
    public void setEntity(TestdataMultipleSourceCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataMultipleSourceCascadingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataMultipleSourceCascadingValue previous) {
        this.previous = previous;
    }

    public TestdataMultipleSourceCascadingValue getNext() {
        return next;
    }

    public void setNext(TestdataMultipleSourceCascadingValue next) {
        this.next = next;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    @Override
    public void setCascadeValue(Integer cascadeValue) {
        this.cascadeValue = cascadeValue;
    }

    @Override
    public Integer getCascadeValue() {
        return cascadeValue;
    }

    @Override
    public Integer getSecondCascadeValue() {
        return secondCascadeValue;
    }

    @Override
    public void setSecondCascadeValue(Integer secondCascadeValue) {
        this.secondCascadeValue = secondCascadeValue;
    }

    @Override
    public int getNumberOfCalls() {
        return numberOfCalls;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        numberOfCalls++;
        if (cascadeValue == null) {
            cascadeValue = value;
        }
        if (secondCascadeValue == null || secondCascadeValue != value + 1) {
            secondCascadeValue = value + 1;
        }
    }

    @Override
    public void reset() {
        numberOfCalls = 0;
    }

    @Override
    public String toString() {
        return "TestdataMultipleCascadeValue{" +
                "value=" + value +
                '}';
    }
}
