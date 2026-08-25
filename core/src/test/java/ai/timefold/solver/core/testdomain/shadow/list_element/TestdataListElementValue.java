package ai.timefold.solver.core.testdomain.shadow.list_element;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataListElementValue extends TestdataObject {

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataListElementEntity entity;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataListElementValue previous;

    int duration = 1;

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataListElementValue() {
    }

    public TestdataListElementValue(String code) {
        super(code);
    }

    public TestdataListElementValue(String code, int duration) {
        super(code);
        this.duration = duration;
    }

    @ShadowSources({ "entity", "previous", "previous.endTime" })
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
        if (startTime == null) {
            return null;
        }
        return startTime + duration;
    }

    public TestdataListElementEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataListElementEntity entity) {
        this.entity = entity;
    }

    public TestdataListElementValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataListElementValue previous) {
        this.previous = previous;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
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
