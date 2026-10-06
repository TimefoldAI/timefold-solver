package ai.timefold.solver.core.testdomain.shadow.list_element;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A list element whose start time is shared with the elements of the same alignment group,
 * the way concurrent visits are.
 */
@PlanningEntity
public class TestdataAlignedListElementValue extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataAlignedListElementEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataAlignedListElementValue previous;

    String alignmentGroup;

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataAlignedListElementValue() {
    }

    public TestdataAlignedListElementValue(String code, String alignmentGroup) {
        super(code);
        this.alignmentGroup = alignmentGroup;
    }

    @ShadowSources(value = { "entity", "previous", "previous.endTime" }, alignmentKey = "alignmentGroup")
    public Integer startTimeSupplier() {
        if (entity == null) {
            return null;
        }
        if (previous != null) {
            return previous.getEndTime();
        }
        return entity.getStartTime();
    }

    @ShadowSources("startTime")
    public Integer endTimeSupplier() {
        return startTime == null ? null : startTime + 1;
    }

    public TestdataAlignedListElementEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataAlignedListElementEntity entity) {
        this.entity = entity;
    }

    public TestdataAlignedListElementValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataAlignedListElementValue previous) {
        this.previous = previous;
    }

    public String getAlignmentGroup() {
        return alignmentGroup;
    }

    public void setAlignmentGroup(String alignmentGroup) {
        this.alignmentGroup = alignmentGroup;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }
}
