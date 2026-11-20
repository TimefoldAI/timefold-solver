package ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;
import ai.timefold.solver.core.testdomain.TestdataValue;

/**
 * A basic (non-list) {@link PlanningVariable} value that is itself a planning entity holding
 * its own basic variable ("owner"). No {@code @InverseRelationShadowVariable} is declared anywhere
 * for the "group" variable of {@link TestdataBasicInverseEntity} that points here.
 */
@PlanningEntity
public class TestdataBasicInverseGroup extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "ownerRange")
    TestdataValue owner;

    public TestdataBasicInverseGroup() {
    }

    public TestdataBasicInverseGroup(String code) {
        super(code);
    }

    public TestdataValue getOwner() {
        return owner;
    }

    public void setOwner(TestdataValue owner) {
        this.owner = owner;
    }
}
