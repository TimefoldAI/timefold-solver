package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.shadow_var.TestdataSingleCascadingSolution;

@PlanningEntity
public class TestdataCascadingDuplicatedMethodAndField {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataSingleCascadingSolution.class, TestdataSingleCascadingEntity.class,
                        TestdataCascadingDuplicatedMethodAndField.class)
                .findEntityDescriptorOrFail(TestdataCascadingDuplicatedMethodAndField.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingDuplicatedMethodAndField previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingDuplicatedMethodAndField next;
    @CascadingUpdateShadowVariable(targetMethodName = "wrongField", sourceVariableName = "entity")
    @CascadingUpdateShadowVariable(targetMethodName = "wrongField", sourceVariableName = "previous")
    private Integer cascadeValue;
    private Integer value;
    private Integer wrongField;

    public TestdataCascadingDuplicatedMethodAndField(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadingDuplicatedMethodAndField getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadingDuplicatedMethodAndField previous) {
        this.previous = previous;
    }

    public TestdataCascadingDuplicatedMethodAndField getNext() {
        return next;
    }

    public void setNext(TestdataCascadingDuplicatedMethodAndField next) {
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

    //---Complex methods---//
    public void wrongField() {
        if (value != null) {
            value = value + 1;
        }
    }
}
