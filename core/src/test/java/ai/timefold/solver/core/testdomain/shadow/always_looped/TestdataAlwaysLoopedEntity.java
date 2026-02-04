package ai.timefold.solver.core.testdomain.shadow.always_looped;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

@PlanningEntity
public class TestdataAlwaysLoopedEntity extends TestdataObject {
    @PlanningVariable
    Integer value;

    @ShadowVariable(supplierName = "isEvenSupplier")
    Boolean even;

    @ShadowVariable(supplierName = "isOddSupplier")
    Boolean odd;

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
        return even;
    }

    public Boolean getOdd() {
        return odd;
    }

    public void setEven(Boolean even) {
        this.even = even;
    }

    public void setOdd(Boolean odd) {
        this.odd = odd;
    }

    // Complex methods
    @ShadowSources({ "value", "odd" })
    public Boolean isEvenSupplier() {
        if (value == null) {
            return null;
        }
        if (odd != null) {
            return !odd;
        }
        return value % 2 == 0;
    }

    @ShadowSources({ "value", "even" })
    public Boolean isOddSupplier() {
        if (value == null) {
            return null;
        }
        if (even != null) {
            return !even;
        }
        return value % 2 == 1;
    }
}
