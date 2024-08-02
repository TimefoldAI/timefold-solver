package ai.timefold.solver.core.impl.testdata.domain.cascade.single_var;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

@PlanningEntity
public class TestdataSingleCascadingValue {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return TestdataSingleCascadingSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataSingleCascadingValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingValue next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    // Having a field with the same name as the targetMethodName should not cause any failure
    // when parsing @CascadingUpdateShadowVariable
    @SuppressWarnings("unused")
    private Integer updateCascadeValue;
    private Integer value;
    private int numberOfCalls = 0;

    public TestdataSingleCascadingValue(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataSingleCascadingValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataSingleCascadingValue previous) {
        this.previous = previous;
    }

    public TestdataSingleCascadingValue getNext() {
        return next;
    }

    public void setNext(TestdataSingleCascadingValue next) {
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

    public void updateCascadeValue(@SuppressWarnings("unused") int i) {
        // Overloaded methods should not cause any failures when parsing @CascadingUpdateShadowVariable
    }

    @Override
    public String toString() {
        return "TestdataSingleCascadingValue{" +
                "value=" + value +
                '}';
    }
}
