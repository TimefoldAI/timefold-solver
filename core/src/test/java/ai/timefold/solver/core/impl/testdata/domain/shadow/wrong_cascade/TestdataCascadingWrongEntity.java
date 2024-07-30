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
public class TestdataCascadingWrongEntity {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataSingleCascadingSolution.class, TestdataSingleCascadingEntity.class,
                        TestdataCascadingWrongEntity.class)
                .findEntityDescriptorOrFail(TestdataCascadingWrongEntity.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingWrongEntity previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingWrongEntity next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue",
            sourceEntityClass = Object.class, sourceVariableName = "valueList")
    private Integer cascadeValue;
    private Integer value;

    public TestdataCascadingWrongEntity(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadingWrongEntity getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadingWrongEntity previous) {
        this.previous = previous;
    }

    public TestdataCascadingWrongEntity getNext() {
        return next;
    }

    public void setNext(TestdataCascadingWrongEntity next) {
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
