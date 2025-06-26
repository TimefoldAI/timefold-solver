package ai.timefold.solver.core.testdomain.declarative.simple_list;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.InverseRelationShadowVariable;
import ai.timefold.solver.core.api.domain.variable.PreviousElementShadowVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataDeclarativeSimpleListValue extends TestdataObject {
    int position;
    int duration;

    @PreviousElementShadowVariable(sourceVariableName = "values")
    TestdataDeclarativeSimpleListValue previous;

    @InverseRelationShadowVariable(sourceVariableName = "values")
    TestdataDeclarativeSimpleListEntity entity;

    @ShadowVariable(supplierName = "startTimeSupplier")
    Integer startTime;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataDeclarativeSimpleListValue() {
    }

    public TestdataDeclarativeSimpleListValue(String code, int position, int duration) {
        super(code);
        this.position = position;
        this.duration = duration;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public TestdataDeclarativeSimpleListValue getPrevious() {
        return previous;
    }

    public void setPrevious(TestdataDeclarativeSimpleListValue previous) {
        this.previous = previous;
    }

    public TestdataDeclarativeSimpleListEntity getEntity() {
        return entity;
    }

    public void setEntity(TestdataDeclarativeSimpleListEntity entity) {
        this.entity = entity;
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

    @ShadowSources({ "entity", "previous.endTime" })
    public Integer startTimeSupplier() {
        if (entity == null) {
            return null;
        }
        if (previous == null) {
            return entity.startTime + Math.abs(position - entity.position);
        }
        return previous.endTime + Math.abs(position - previous.position);
    }

    @ShadowSources("startTime")
    public Integer endTimeSupplier() {
        if (startTime == null) {
            return null;
        }
        return startTime + duration;
    }

}
