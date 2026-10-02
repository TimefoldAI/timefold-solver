package ai.timefold.solver.core.testdomain.shadow.list_element;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * A list element without any element ordering shadow variables;
 * its declarative shadow variable depends on a genuine basic variable instead.
 */
@PlanningEntity
public class TestdataMixedListElementValue extends TestdataObject {

    @PlanningVariable(allowsUnassigned = true)
    Integer duration;

    @ShadowVariable(supplierName = "paddedDurationSupplier")
    Integer paddedDuration;

    public TestdataMixedListElementValue() {
    }

    public TestdataMixedListElementValue(String code) {
        super(code);
    }

    @ShadowSources("duration")
    public Integer paddedDurationSupplier() {
        return duration == null ? null : duration + 1;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getPaddedDuration() {
        return paddedDuration;
    }

    public void setPaddedDuration(Integer paddedDuration) {
        this.paddedDuration = paddedDuration;
    }
}
