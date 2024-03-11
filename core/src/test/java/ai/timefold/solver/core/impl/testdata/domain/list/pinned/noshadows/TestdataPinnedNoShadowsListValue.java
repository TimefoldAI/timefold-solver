package ai.timefold.solver.core.impl.testdata.domain.list.pinned.noshadows;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.index.IndexShadowVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.InverseRelationShadowVariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataPinnedNoShadowsListValue extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedNoShadowsListSolution> buildEntityDescriptor() {
        return TestdataPinnedNoShadowsListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedNoShadowsListValue.class);
    }

    public static InverseRelationShadowVariableDescriptor<TestdataPinnedNoShadowsListSolution>
            buildVariableDescriptorForEntity() {
        return (InverseRelationShadowVariableDescriptor<TestdataPinnedNoShadowsListSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("entity");
    }

    public static IndexShadowVariableDescriptor<TestdataPinnedNoShadowsListSolution> buildVariableDescriptorForIndex() {
        return (IndexShadowVariableDescriptor<TestdataPinnedNoShadowsListSolution>) buildEntityDescriptor()
                .getShadowVariableDescriptor("index");
    }

    // Intentionally missing the inverse relation variable.
    @IndexShadowVariable(sourceVariableName = "valueList")
    private Integer index;

    public TestdataPinnedNoShadowsListValue() {
    }

    public TestdataPinnedNoShadowsListValue(String code) {
        super(code);
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
