package ai.timefold.solver.core.testdomain.shadow.diamond;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * The shadow variables form a diamond: {@code value} feeds both {@code duration} and {@code endTime},
 * and {@code duration} feeds {@code endTime} as well.
 * Since no source crosses an entity boundary, the variable reference graph is fixed.
 */
@PlanningEntity
public class TestdataDiamondEntity extends TestdataObject {

    @PlanningVariable
    TestdataDiamondValue value;

    @ShadowVariable(supplierName = "durationSupplier")
    Integer duration;

    @ShadowVariable(supplierName = "endTimeSupplier")
    Integer endTime;

    public TestdataDiamondEntity() {
    }

    public TestdataDiamondEntity(String code) {
        super(code);
    }

    public TestdataDiamondValue getValue() {
        return value;
    }

    public void setValue(TestdataDiamondValue value) {
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
