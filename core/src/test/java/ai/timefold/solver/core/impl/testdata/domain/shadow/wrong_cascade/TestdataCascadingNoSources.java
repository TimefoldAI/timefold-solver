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
public class TestdataCascadingNoSources {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataSingleCascadingSolution.class, TestdataSingleCascadingEntity.class,
                        TestdataCascadingNoSources.class)
                .findEntityDescriptorOrFail(TestdataCascadingNoSources.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingNoSources previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingNoSources next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    private Integer value;

    public TestdataCascadingNoSources(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadingNoSources getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadingNoSources previous) {
        this.previous = previous;
    }

    public TestdataCascadingNoSources getNext() {
        return next;
    }

    public void setNext(TestdataCascadingNoSources next) {
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
    public void updateCascadeValue() {
        if (value != null) {
            value = value + 1;
        }
    }
}
