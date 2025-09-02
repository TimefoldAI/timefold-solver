package ai.timefold.solver.core.testdomain.declarative.always_looped;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowVariablesInconsistent;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataAlwaysLoopedEntity extends TestdataObject {
    @PlanningVariable
    Integer value;

    // TODO: Remove me when supplier present
    @ShadowVariablesInconsistent
    boolean isInconsistent;

    @ShadowVariable(supplierName = "isEvenSupplier")
    Boolean isEven;

    @ShadowVariable(supplierName = "isOddSupplier")
    Boolean isOdd;

    public TestdataAlwaysLoopedEntity() {
    }

    public TestdataAlwaysLoopedEntity(String id) {
        super(id);
    }

    public TestdataAlwaysLoopedEntity(String id, int value) {
        this(id);
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Boolean getEven() {
        return isEven;
    }

    public Boolean getOdd() {
        return isOdd;
    }

    // Complex methods
    @ShadowSources({ "value", "isOdd" })
    public Boolean isEvenSupplier() {
        if (value == null) {
            return null;
        }
        if (isOdd != null) {
            return !isOdd;
        }
        return value % 2 == 0;
    }

    @ShadowSources({ "value", "isEven" })
    public Boolean isOddSupplier() {
        if (value == null) {
            return null;
        }
        if (isEven != null) {
            return !isEven;
        }
        return value % 2 == 1;
    }
}
