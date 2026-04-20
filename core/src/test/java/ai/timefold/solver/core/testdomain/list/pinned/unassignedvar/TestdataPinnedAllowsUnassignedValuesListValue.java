package ai.timefold.solver.core.testdomain.list.pinned.unassignedvar;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.IndexShadowVariable;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.NextElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataPinnedAllowsUnassignedValuesListValue extends TestdataObject {

    private TestdataPinnedAllowsUnassignedValuesListEntity entity;
    private Integer index;
    private TestdataPinnedAllowsUnassignedValuesListValue previous;
    private TestdataPinnedAllowsUnassignedValuesListValue next;

    public TestdataPinnedAllowsUnassignedValuesListValue() {
    }

    public TestdataPinnedAllowsUnassignedValuesListValue(String code) {
        super(code);
    }

    @InverseRelationShadowVariable(sourceVariableName = "valueList")
    public TestdataPinnedAllowsUnassignedValuesListEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataPinnedAllowsUnassignedValuesListEntity entity) {
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
    public TestdataPinnedAllowsUnassignedValuesListValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataPinnedAllowsUnassignedValuesListValue previous) {
        this.previous = previous;
    }

    @NextElementShadowVariable(sourceVariableName = "valueList")
    public TestdataPinnedAllowsUnassignedValuesListValue getNext() {
        return next;
    }

    public void setNext(TestdataPinnedAllowsUnassignedValuesListValue next) {
        this.next = next;
    }
}
