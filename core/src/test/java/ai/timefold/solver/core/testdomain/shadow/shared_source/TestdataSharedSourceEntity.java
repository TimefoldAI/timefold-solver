package ai.timefold.solver.core.testdomain.shadow.shared_source;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * {@code duration} and {@code endTime} both source from {@code value}, and {@code endTime} also
 * sources from {@code duration}. Since no source crosses an entity boundary, the variable
 * reference graph is fixed.
 */
@PlanningEntity
public class TestdataSharedSourceEntity extends TestdataObject {

    @PlanningVariable
    TestdataSharedSourceValue value;

    @ShadowVariable(supplierName = "durationSupplier")
    Integer duration;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataSharedSourceEntity() {
    }

    public TestdataSharedSourceEntity(String code) {
        super(code);
    }

    public TestdataSharedSourceValue getValue() {
        return value;
    }

    public void setValue(TestdataSharedSourceValue value) {
        this.value = value;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getEndTime() {
        return endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

    @ShadowSources("value")
    public Integer durationSupplier() {
        return value == null ? null : value.getDuration();
    }

    @ShadowSources({ "value", "duration" })
    public Integer endTimeSupplier() {
        if (value == null || duration == null) {
            return null;
        }
        return value.getStartTime() + duration;
    }

}
