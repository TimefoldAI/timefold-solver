package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadeEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadeSolution;

@PlanningEntity
public class TestdataCascadeMissingInverseValue {

    public static EntityDescriptor<TestdataSingleCascadeSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataSingleCascadeSolution.class, TestdataSingleCascadeEntity.class,
                        TestdataCascadeMissingInverseValue.class)
                .findEntityDescriptorOrFail(TestdataCascadeMissingInverseValue.class);
    }

    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeMissingInverseValue previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeMissingInverseValue next;
    @CascadeUpdateElementShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadeUpdateElementShadowVariable(targetMethodName = "updateCascadeValueWithReturnType")
    private Integer cascadeValueReturnType;
    private Integer value;

    public TestdataCascadeMissingInverseValue(Integer value) {
        this.value = value;
    }

    public TestdataCascadeMissingInverseValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadeMissingInverseValue previous) {
        this.previous = previous;
    }

    public TestdataCascadeMissingInverseValue getNext() {
        return next;
    }

    public void setNext(TestdataCascadeMissingInverseValue next) {
        this.next = next;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getCascadeValue() {
        return cascadeValue;
    }

    public Integer getCascadeValueReturnType() {
        return cascadeValueReturnType;
    }

    //---Complex methods---//
    public void updateCascadeValue() {
        if (value != null) {
            value = value + 1;
        }
    }

    public Integer updateCascadeValueWithReturnType() {
        updateCascadeValue();
        cascadeValueReturnType = cascadeValue;
        return cascadeValueReturnType;
    }
}
