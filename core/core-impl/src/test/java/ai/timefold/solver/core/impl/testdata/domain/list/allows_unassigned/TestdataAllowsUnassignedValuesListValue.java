package ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataObject;

@PlanningEntity
public class TestdataAllowsUnassignedValuesListValue extends TestdataObject {

    public static EntityDescriptor<TestdataAllowsUnassignedValuesListSolution> buildEntityDescriptor() {
        return TestdataAllowsUnassignedValuesListSolution.buildSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataAllowsUnassignedValuesListValue.class);
    }

    private TestdataAllowsUnassignedValuesListEntity entity;
    private Integer index;
    private TestdataAllowsUnassignedValuesListValue previous;
    private TestdataAllowsUnassignedValuesListValue next;

    public TestdataAllowsUnassignedValuesListValue() {
    }

    public TestdataAllowsUnassignedValuesListValue(String code) {
        super(code);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    public TestdataAllowsUnassignedValuesListEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataAllowsUnassignedValuesListEntity entity) {
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
    public TestdataAllowsUnassignedValuesListValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataAllowsUnassignedValuesListValue previous) {
        this.previous = previous;
    }

    @NextElementShadowVariable(sourceVariableName = "valueList")
    public TestdataAllowsUnassignedValuesListValue getNext() {
        return next;
    }

    public void setNext(TestdataAllowsUnassignedValuesListValue next) {
        this.next = next;
    }
}
