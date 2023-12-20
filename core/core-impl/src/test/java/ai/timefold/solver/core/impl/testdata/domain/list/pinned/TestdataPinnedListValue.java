package ai.timefold.solver.core.impl.testdata.domain.list.pinned;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataPinnedListValue extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedListSolution> buildEntityDescriptor() {
        return TestdataPinnedListSolution.buildSolutionDescriptor().findEntityDescriptorOrFail(TestdataPinnedListValue.class);
    }

    public static InverseRelationShadowVariableDescriptor<TestdataPinnedListSolution> buildVariableDescriptorForEntity() {
        return (InverseRelationShadowVariableDescriptor<TestdataPinnedListSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("entity");
    }

    public static IndexShadowVariableDescriptor<TestdataPinnedListSolution> buildVariableDescriptorForIndex() {
        return (IndexShadowVariableDescriptor<TestdataPinnedListSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("index");
    }

    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;

    public TestdataPinnedListValue() {
    }

    public TestdataPinnedListValue(String code) {
        super(code);
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
