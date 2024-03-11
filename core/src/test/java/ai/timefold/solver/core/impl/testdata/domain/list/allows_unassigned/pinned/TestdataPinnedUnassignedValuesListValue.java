package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataPinnedUnassignedValuesListValue extends TestdataObject {

    public static EntityDescriptor<TestdataPinnedUnassignedValuesListSolution> buildEntityDescriptor() {
        return TestdataPinnedUnassignedValuesListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataPinnedUnassignedValuesListValue.class);
    }

    private TestdataPinnedUnassignedValuesListEntity entity;
    private Integer index;
    private TestdataPinnedUnassignedValuesListValue previous;
    private TestdataPinnedUnassignedValuesListValue next;

    public TestdataPinnedUnassignedValuesListValue() {
    }

    public TestdataPinnedUnassignedValuesListValue(String code) {
        super(code);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    public TestdataPinnedUnassignedValuesListEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataPinnedUnassignedValuesListEntity entity) {
        this.entity = entity;
    }

    @IndexShadowVariable(sourceVariableName = "valueList")
    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @PreviousElementShadowVariable(sourceVariableName = "valueList")
    public TestdataPinnedUnassignedValuesListValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataPinnedUnassignedValuesListValue previous) {
        this.previous = previous;
    }

    @NextElementShadowVariable(sourceVariableName = "valueList")
    public TestdataPinnedUnassignedValuesListValue getNext() {
        return next;
    }

    public void setNext(TestdataPinnedUnassignedValuesListValue next) {
        this.next = next;
    }
}
