package ai.timefold.solver.core.testdomain.shadow.declarative.basicinverse;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.api.domain.variable.ShadowSources;
import ai.timefold.solver.core.api.domain.variable.ShadowVariable;
import ai.timefold.solver.core.testdomain.TestdataObject;

/**
 * The "group" variable has no {@code @InverseRelationShadowVariable} declared for it anywhere
 * in this model. The declarative "ownerCode" shadow chains through two genuine (non-declarative)
 * variables ("group", then "group.owner"), which forces the framework to resolve "which entities
 * currently point to a given group" via a fresh, non-externalized (map-mode)
 * {@code BasicVariableStateDemand}/{@code ExternalizedBasicVariableStateSupply}, since no annotated
 * inverse field exists to reuse.
 */
@PlanningEntity
public class TestdataBasicInverseEntity extends TestdataObject {

    @PlanningVariable(valueRangeProviderRefs = "groupRange")
    TestdataBasicInverseGroup group;

    @ShadowVariable(supplierName = "updateOwnerCode")
    String ownerCode;

    public TestdataBasicInverseEntity() {
    }

    public TestdataBasicInverseEntity(String code) {
        super(code);
    }

    public TestdataBasicInverseGroup getGroup() {
        return group;
    }

    public void setGroup(TestdataBasicInverseGroup group) {
        this.group = group;
    }

    public String getOwnerCode() {
        return ownerCode;
    }

    public void setOwnerCode(String ownerCode) {
        this.ownerCode = ownerCode;
    }

    @ShadowSources("group.owner")
    public String updateOwnerCode() {
        if (group == null || group.getOwner() == null) {
            return null;
        }
        return group.getOwner().getCode();
    }
}
