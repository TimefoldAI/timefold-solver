package ai.timefold.solver.core.impl.testdata.domain.list.pinned.boxed;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataBoxedPinnedWithIndexListValue extends TestdataObject {

    public static EntityDescriptor<TestdataBoxedPinnedWithIndexListSolution> buildEntityDescriptor() {
        return TestdataBoxedPinnedWithIndexListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataBoxedPinnedWithIndexListValue.class);
    }

    public static InverseRelationShadowVariableDescriptor<TestdataBoxedPinnedWithIndexListSolution>
            buildVariableDescriptorForEntity() {
        return (InverseRelationShadowVariableDescriptor<TestdataBoxedPinnedWithIndexListSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("entity");
    }

    public static IndexShadowVariableDescriptor<TestdataBoxedPinnedWithIndexListSolution> buildVariableDescriptorForIndex() {
        return (IndexShadowVariableDescriptor<TestdataBoxedPinnedWithIndexListSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("index");
    }

    // Index shadow var intentionally missing, to test that the supply can deal with that.
    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    private TestdataBoxedPinnedWithIndexListEntity entity;

    public TestdataBoxedPinnedWithIndexListValue() {
    }

    public TestdataBoxedPinnedWithIndexListValue(String code) {
        super(code);
    }

    public TestdataBoxedPinnedWithIndexListEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataBoxedPinnedWithIndexListEntity entity) {
        this.entity = entity;
    }

}
