package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadeUpdateElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeSolution;
import ai.timefold.solver.core.impl.testdata.domain.cascade.TestdataCascadeValue;

@PlanningEntity
public class TestdataCascadeMissingNextValue {

    public static EntityDescriptor<TestdataCascadeSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataCascadeSolution.class, TestdataCascadeEntity.class,
                        TestdataCascadeMissingNextValue.class)
                .findEntityDescriptorOrFail(TestdataCascadeMissingNextValue.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadeValue previous;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @CascadeUpdateElementShadowVariable(sourceMethodName = "updateCascadeValueWithReturnType")
    private Integer cascadeValueReturnType;
    private Integer value;

    public TestdataCascadeMissingNextValue(Integer value) {
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
