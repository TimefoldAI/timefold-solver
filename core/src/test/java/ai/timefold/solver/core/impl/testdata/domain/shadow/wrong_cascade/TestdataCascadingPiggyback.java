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
public class TestdataCascadingPiggyback {

    public static EntityDescriptor<TestdataSingleCascadingSolution> buildEntityDescriptor() {
        return SolutionDescriptor
                .buildSolutionDescriptor(TestdataSingleCascadingSolution.class, TestdataSingleCascadingEntity.class,
                        TestdataCascadingPiggyback.class)
                .findEntityDescriptorOrFail(TestdataCascadingPiggyback.class);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataSingleCascadingEntity entity;
    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingPiggyback previous;
    @NextElementShadowVariable(sourceVariableName = "valueList")
    private TestdataCascadingPiggyback next;
    @CascadingUpdateShadowVariable(targetMethodName = "updateCascadeValue")
    private Integer cascadeValue;
    @PiggybackShadowVariable(shadowVariableName = "cascadeValue")
    private Integer cascadeValue2;
    private Integer value;

    public TestdataCascadingPiggyback(Integer value) {
        this.value = value;
    }

    public TestdataSingleCascadingEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataSingleCascadingEntity entity) {
        this.entity = entity;
    }

    public TestdataCascadingPiggyback getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataCascadingPiggyback previous) {
        this.previous = previous;
    }

    public TestdataCascadingPiggyback getNext() {
        return next;
    }

    public void setNext(TestdataCascadingPiggyback next) {
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
