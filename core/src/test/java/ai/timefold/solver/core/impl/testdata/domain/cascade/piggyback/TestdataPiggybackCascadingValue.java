package ai.timefold.solver.core.impl.testdata.domain.cascade.piggyback;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataPiggybackCascadingValue {

    public static EntityDescriptor<TestdataPiggybackCascadingSolution> buildEntityDescriptor() {
        return TestdataPiggybackCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPiggybackCascadingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataPiggybackCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataPiggybackCascadingValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataPiggybackCascadingValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @PiggybackShadowVariable(shadowVariableName = "cascadeValue")
    private Integer secondCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataPiggybackCascadingValue(Integer value) {
        this.value = value;
    }

    public TestdataPiggybackCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataPiggybackCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataPiggybackCascadingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataPiggybackCascadingValue previous) {
        this.previous = previous;
    }

    public TestdataPiggybackCascadingValue getNext() {
        return next;
    }

    public void setNext(TestdataPiggybackCascadingValue next) {
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

    public void reset() {
        numberOfCalls = 0;
    }

    @Override
    public String toString() {
        return "TestdataPiggybackCascadingValue{" +
                "value=" + value +
                '}';
    }
}
