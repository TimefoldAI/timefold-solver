package ai.timefold.solver.core.impl.testdata.domain.shadow.wrong_cascade;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.CascadingUpdateShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PiggybackShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadingEntity;
import ai.timefold.solver.core.impl.testdata.domain.cascade.single_var.TestdataSingleCascadingSolution;

@PlanningEntity
public class TestdataCascadingInvalidPiggyback {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataSingleCascadingSolution.class, TestdataSingleCascadingEntity.class,
                        TestdataCascadingInvalidPiggyback.class)
                .findEntityDescriptorOrFail(TestdataCascadingInvalidPiggyback.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingInvalidPiggyback previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingInvalidPiggyback next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @PiggybackShadowVariable(shadowVariableName = "cascadeValue")
    private Integer cascadeValue2;
    private Integer value;

    public TestdataCascadingInvalidPiggyback(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadingInvalidPiggyback getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadingInvalidPiggyback previous) {
        this.previous = previous;
    }

    public TestdataCascadingInvalidPiggyback getNext() {
        return next;
    }

    public void setNext(TestdataCascadingInvalidPiggyback next) {
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
